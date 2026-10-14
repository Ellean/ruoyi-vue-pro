package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.hutool.core.codec.Base64;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.openai.OpenAiCompatibleImageEditApi;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiUploadImageReqVO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_AI_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_TRIGGER_FAIL;

/**
 * 主 API：文案走默认对话模型；识图配置只下发给 RPA，原图不进本进程。
 */
@Service
@Validated
@Slf4j
public class XqRpaAiServiceImpl implements XqRpaAiService {

    private static final Set<String> IMAGE_TYPE_SET = Set.of(
            "main", "dimension", "detail", "scene", "package", "other");
    private static final Map<String, String> TYPE_MARKERS = Map.of(
            "main", "主图-突出卖点与外形",
            "dimension", "尺寸图-产品外形+标注尺寸（须双语绑定）",
            "detail", "细节图-材质/工艺特写",
            "scene", "场景图-使用情境",
            "package", "包装图-开箱/配件",
            "other", "其它参考图"
    );
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}|\\[[\\s\\S]*]");

    @Value("${xq.rpa.callback-token:xq-rpa-callback}")
    private String callbackToken;
    @Value("${xq.ai.mock-when-empty:false}")
    private boolean mockWhenEmpty;

    @Resource
    private AiModelService aiModelService;
    @Resource
    private AiApiKeyService aiApiKeyService;
    @Resource
    private FileService fileService;
    @Resource
    private XqAiUsageLogService aiUsageLogService;

    /** 当前线程 AI 调用累计（文案多轮 chat 合并记一条） */
    private static final ThreadLocal<UsageAcc> USAGE_ACC = new ThreadLocal<>();

    private static final class UsageAcc {
        private Long workOrderId;
        private String category;
        private String model;
        private int promptTokens;
        private int completionTokens;
        private int calls;
    }

    private void beginUsage(Long workOrderId, String category) {
        if (workOrderId == null) {
            return;
        }
        UsageAcc acc = new UsageAcc();
        acc.workOrderId = workOrderId;
        acc.category = category;
        USAGE_ACC.set(acc);
    }

    private void flushUsage(String remark) {
        UsageAcc acc = USAGE_ACC.get();
        USAGE_ACC.remove();
        if (acc == null || acc.workOrderId == null) {
            return;
        }
        // mock / 未真正调模型时不记账
        if (acc.calls <= 0 && acc.promptTokens <= 0 && acc.completionTokens <= 0) {
            return;
        }
        int calls = Math.max(1, acc.calls);
        aiUsageLogService.record(acc.workOrderId, acc.category, acc.model,
                acc.promptTokens, acc.completionTokens, calls, null, remark);
    }

    @Override
    public Map<String, Object> generateCopy(XqRpaAiGenerateCopyReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        beginUsage(reqVO == null ? null : reqVO.getWorkOrderId(), XqAiUsageLogService.CAT_COPY);
        try {
            return doGenerateCopy(reqVO);
        } finally {
            flushUsage("generate-copy");
        }
    }

    private Map<String, Object> doGenerateCopy(XqRpaAiGenerateCopyReqVO reqVO) {
        CopyRule rule = parseCopyRule(reqVO.getCopyRule());
        String sku = StrUtil.blankToDefault(reqVO.getSku(), "");
        String title0 = StrUtil.blankToDefault(reqVO.getTitle(), sku);
        String original = StrUtil.blankToDefault(reqVO.getOriginalCopy(), "");
        boolean mock = Boolean.TRUE.equals(reqVO.getMock());
        Map<String, Object> observations = reqVO.getImageObservations() == null
                ? Map.of() : reqVO.getImageObservations();
        List<String> sourceImages = CollUtil.emptyIfNull(reqVO.getSourceImages());
        // 有原文案即可写文案；识图观察可选（全图识图留给生图机器人，避免文案流水线被拖慢）
        if (!mock && StrUtil.isBlank(original) && observations.isEmpty() && StrUtil.isBlank(title0)) {
            throw exception(XQ_RPA_AI_FAIL, "文案生成需要原文案、标题或首图识图结果");
        }

        if (mock) {
            return mockCopy(sku, title0, original, rule);
        }

        Map<String, Object> copyPrompts = normalizeCopyPrompts(reqVO.getCopyPrompts());
        boolean hasCopyPrompts = !copyPrompts.isEmpty();
        boolean regenerate = Boolean.TRUE.equals(reqVO.getRegenerate())
                || StrUtil.isNotBlank(reqVO.getRevisionPrompt())
                || (reqVO.getExistingCopy() != null && !reqVO.getExistingCopy().isEmpty());
        String revisionPrompt = StrUtil.blankToDefault(reqVO.getRevisionPrompt(), "");
        // 无整段 revisionPrompt 时，把分项提示词拼成写作指引
        if (StrUtil.isBlank(revisionPrompt) && hasCopyPrompts) {
            revisionPrompt = buildRevisionFromCopyPrompts(copyPrompts);
        }
        Map<String, Object> existingCopy = reqVO.getExistingCopy() == null
                ? Map.of() : reqVO.getExistingCopy();
        String system = "你是亚马逊/跨境电商文案优化助手。文案生成与图片提示词生成是两个独立概念："
                + "本任务只生成文案，不写生图提示词。"
                + "只输出一个完整 JSON，禁止截断、禁止 markdown。"
                // 卖点放最前：即使后半 description 被截断，也能先解析到 sellingPoints
                + "字段顺序必须是：{\"sellingPoints\":[\"完整英文句子\"],\"title\":\"完整英文标题\","
                + "\"description\":\"完整英文长描述\",\"highlightStyle\":[\"中文特征点\"]}。"
                + "输入优先：1) originalCopy 原文案；2) productTitle；3) 可选 imageObservations（可能为空，勿强依赖）；"
                + "4) 可选 copyPrompts 分项写作提示词（title/features/description）。"
                + "必须依据原文案与标题写 title/sellingPoints/description；没有识图结果时不要编造图上看不到的材质、配件、尺寸、认证。"
                + "三段文案都必须遵守各自规则：rules.titleLayout、rules.descriptionLayout、rules.featureLayout，以及 rules.hints。"
                + "可参考各 layout.example 作为输出结构样板。"
                + "sellingPoints：最先输出；条数必须等于 featureCount；每条尽量 ≤220 字符；若 featureLayout.splitTitleContent=true，"
                + "每条格式为 \"ShortTitle\" + separator + \"完整英文内容句\"（如 Easy Touch Control: ...）；"
                + "否则每条为完整英文句并以句号结尾；遵守 featureTitleMaxLen / featureContentMaxLen。"
                + "title：英文标题，字数在 titleMinLen～titleMaxLen；若 titleNoPunctuation=true 则不要逗号/句号等标点。"
                + "description：纯文本（字符串内用 \\n 换行，不要 markdown 代码块）；控制在 900 词内。"
                + "若 descriptionLayout.mode=paragraph：只写 4～8 句营销段落，不要规格块。"
                + "否则（amazonLong）：先写 2～4 句导语；空一行；写 specsHeading；"
                + "再写 \"- Key: Value\" 规格行（minSpecItems～maxSpecItems）；"
                + "includeImportantNotes=true 时再追加 notesHeading 与注意事项。"
                + "规格只能来自原文案/标题可核实属性，禁止编造。"
                + "highlightStyle：4～8 个中文短标签（2～8 字），禁止英文与整句。"
                + "不要输出 imagePrompts。";
        String target = StrUtil.blankToDefault(reqVO.getTarget(), "all").trim().toLowerCase();
        Integer featureIndex = reqVO.getFeatureIndex();
        if (hasCopyPrompts) {
            system += "若提供 copyPrompts：写 title 时遵守 copyPrompts.title；"
                    + "写 sellingPoints[i] 时遵守 copyPrompts.features[i]（有则用）；"
                    + "写 description 时遵守 copyPrompts.description。提示词是写作要求不是最终英文成品，"
                    + "仍须输出合规的英文 title/sellingPoints/description。";
        }
        if (regenerate) {
            system += "这是二次改写任务：必须参考 existingCopy（当前标题/卖点/长描述）和 revisionPrompt（用户提示词）。";
            if ("title".equals(target)) {
                system += "只改写 title，sellingPoints/description/highlightStyle 必须原样返回 existingCopy 中的值。";
            } else if ("description".equals(target)) {
                system += "只改写 description，title/sellingPoints/highlightStyle 必须原样返回 existingCopy 中的值。";
            } else if ("feature".equals(target)) {
                system += "只改写 sellingPoints 中下标为 featureIndex 的那一条，其余卖点与 title/description/highlightStyle 必须原样返回。";
            } else {
                system += "未要求改动的部分尽量保留；按提示词做针对性优化，不要无视现有文案整篇重写。";
            }
        }
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("sku", sku);
        user.put("productTitle", title0);
        user.put("originalCopy", StrUtil.maxLength(original, 4000));
        user.put("regenerate", regenerate);
        user.put("revisionPrompt", StrUtil.maxLength(revisionPrompt, 4000));
        user.put("existingCopy", existingCopy);
        user.put("target", target);
        if (hasCopyPrompts) {
            user.put("copyPrompts", copyPrompts);
        }
        if (featureIndex != null) {
            user.put("featureIndex", featureIndex);
        }
        // 只传首图观察的关键字段，避免把整包 vision 元数据塞进文案模型
        user.put("imageObservations", slimObservations(observations));
        user.put("sourceImageCount", sourceImages.size());
        user.put("featureCount", rule.featureCount);
        // 传精简规则：含各段 hint + 描述规格版式（不传整份 raw JSON）
        user.put("rules", buildCopyRulesPayload(rule));
        user.put("imageCount", CollUtil.size(reqVO.getSourceImages()));

        // 8 条卖点 + 长描述英文成品常超 3k tokens；过小会截断成半截 JSON，解析后卖点为空
        String raw = chat(system, JSONUtil.toJsonStr(user), 6144);
        JSONObject parsed = extractJsonObject(raw);
        List<String> points = pickSellingPoints(parsed, raw);
        String title = clipAtSentence(StrUtil.blankToDefault(firstStr(parsed, "title", "productTitle"), title0),
                rule.titleMaxLen);
        String desc = clipAtSentence(
                StrUtil.blankToDefault(firstStr(parsed, "description", "longDescription", "desc"), ""),
                Math.max(rule.descriptionMaxLen, 2000));
        // 全量生成时禁止用 Key benefit 占位冒充成功（模型截断/空卖点）
        boolean fullGenerate = !regenerate || "all".equals(target);
        if (fullGenerate && (points.isEmpty() || StrUtil.isBlank(desc))) {
            log.warn("[generateCopy] incomplete sku={} points={} descLen={} keys={} rawTail={}",
                    sku, points.size(), StrUtil.length(desc), parsed.keySet(),
                    StrUtil.maxLength(StrUtil.blankToDefault(raw, ""), 500));
            // 自动补全一次：只要求补齐缺失字段，避免整篇重跑
            String fixSystem = "你是 JSON 修复器。根据 previous 半截结果与产品信息，输出完整合法 JSON，"
                    + "字段顺序：sellingPoints,title,description,highlightStyle。"
                    + "sellingPoints 条数必须等于 featureCount；禁止 markdown；禁止截断。";
            Map<String, Object> fixUser = new LinkedHashMap<>();
            fixUser.put("featureCount", rule.featureCount);
            fixUser.put("productTitle", title0);
            fixUser.put("originalCopy", StrUtil.maxLength(original, 2500));
            fixUser.put("previous", StrUtil.maxLength(raw, 5000));
            fixUser.put("needSellingPoints", points.isEmpty());
            fixUser.put("needDescription", StrUtil.isBlank(desc));
            String fixedRaw = chat(fixSystem, JSONUtil.toJsonStr(fixUser), 4096);
            JSONObject fixed = extractJsonObject(fixedRaw);
            if (points.isEmpty()) {
                points = pickSellingPoints(fixed, fixedRaw);
            }
            if (StrUtil.isBlank(title)) {
                title = clipAtSentence(firstStr(fixed, "title", "productTitle"), rule.titleMaxLen);
            }
            if (StrUtil.isBlank(desc)) {
                desc = clipAtSentence(firstStr(fixed, "description", "longDescription", "desc"),
                        Math.max(rule.descriptionMaxLen, 2000));
            }
            if (!fixed.isEmpty()) {
                parsed = fixed;
                raw = fixedRaw;
            }
        }
        if (fullGenerate) {
            if (points.isEmpty()) {
                log.warn("[generateCopy] empty sellingPoints after retry sku={} rawHead={}",
                        sku, StrUtil.maxLength(StrUtil.blankToDefault(raw, ""), 400));
                throw exception(XQ_RPA_AI_FAIL, "模型未返回卖点（可能被截断），请重试或换更快模型");
            }
            if (StrUtil.isBlank(desc)) {
                log.warn("[generateCopy] empty description after retry sku={} rawHead={}",
                        sku, StrUtil.maxLength(StrUtil.blankToDefault(raw, ""), 400));
                throw exception(XQ_RPA_AI_FAIL, "模型未返回长描述（可能被截断），请重试或换更快模型");
            }
        }
        points = padPoints(points, rule.featureCount, Math.max(rule.featureMaxLen, 400));
        List<String> highlightTags = normalizeHighlightTags(parsed.get("highlightStyle"));
        if (highlightTags.isEmpty()) {
            // 优先回填首图 styleTags，避免空标签落入通用 fallback
            highlightTags = normalizeHighlightTags(observations.get("styleTags"));
        }
        if (highlightTags.isEmpty()) {
            highlightTags = fallbackHighlightTags(points, desc);
        }

        // 局部改写：服务端再强制合并，防止模型误改其它字段
        if (regenerate && !"all".equals(target)) {
            String existTitle = str(existingCopy.get("title"));
            String existDesc = str(existingCopy.get("description"));
            List<String> existPoints = normalizePoints(existingCopy.get("sellingPoints"));
            existPoints = padPoints(existPoints, rule.featureCount, Math.max(rule.featureMaxLen, 400));
            if ("title".equals(target)) {
                points = existPoints;
                desc = StrUtil.blankToDefault(existDesc, desc);
            } else if ("description".equals(target)) {
                title = StrUtil.blankToDefault(existTitle, title);
                points = existPoints;
            } else if ("feature".equals(target)) {
                title = StrUtil.blankToDefault(existTitle, title);
                desc = StrUtil.blankToDefault(existDesc, desc);
                int idx = featureIndex == null ? 0 : featureIndex;
                List<String> merged = new ArrayList<>(existPoints);
                while (merged.size() < rule.featureCount) {
                    merged.add("");
                }
                String newPoint = points.size() > idx ? points.get(idx) : "";
                if (StrUtil.isBlank(newPoint) && !points.isEmpty()) {
                    newPoint = points.get(0);
                }
                if (idx >= 0 && idx < merged.size() && StrUtil.isNotBlank(newPoint)) {
                    merged.set(idx, newPoint);
                }
                points = padPoints(merged, rule.featureCount, Math.max(rule.featureMaxLen, 400));
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("sellingPoints", points);
        out.put("description", desc);
        out.put("highlightStyle", highlightTags);
        // 与 highlightStyle 同义，供 RPA 图片二创阶段显式读取中文特征点
        out.put("featurePoints", highlightTags);
        return out;
    }

    @Override
    public Map<String, Object> getVisionProfile(String callbackToken) {
        assertToken(callbackToken);
        // 收集所有启用且具备识图能力的 Chat API，供机器人一图一聊轮询
        List<Map<String, Object>> profiles = listVisionProfiles();
        if (profiles.isEmpty()) {
            throw exception(XQ_RPA_AI_FAIL,
                    "找不到可用的识图 Chat API。请到【API 密钥】启用至少一条带 vision 能力的密钥，"
                            + "并填写「识图模型」（如 gpt-4o / gpt-5.6-terra）");
        }
        // 兼容旧字段：顶层仍返回第一条；profiles = 全部可用识图 API
        Map<String, Object> out = new LinkedHashMap<>(profiles.get(0));
        out.put("profiles", profiles);
        out.put("profileCount", profiles.size());
        return out;
    }

    /**
     * 列出所有可识图的 Chat API（启用 + capabilities 含 vision / 已配 visionModel，且模型非纯文本）。
     * 默认对话模型绑定的密钥排在最前，便于稳定优先。
     * <p>
     * 严格只用「识图模型 / 对话模型」；绝不使用 imageModel（生图如 gpt-image-*）。
     */
    private List<Map<String, Object>> listVisionProfiles() {
        Long preferredKeyId = null;
        String preferredChatModelName = null;
        Double preferredTemp = 0.3;
        Integer preferredMaxTokens = 4096;
        String preferredPlatform = null;
        try {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            preferredKeyId = model.getKeyId();
            preferredChatModelName = model.getModel();
            preferredTemp = model.getTemperature() != null ? model.getTemperature() : 0.3;
            preferredMaxTokens = model.getMaxTokens() != null ? model.getMaxTokens() : 4096;
            preferredPlatform = model.getPlatform();
        } catch (Exception ignored) {
            // 无默认模型时仍可从密钥池组装
        }

        List<AiApiKeyDO> keys = CollUtil.emptyIfNull(aiApiKeyService.getApiKeyList());
        List<Map<String, Object>> preferred = new ArrayList<>();
        List<Map<String, Object>> others = new ArrayList<>();
        for (AiApiKeyDO key : keys) {
            if (key == null || !CommonStatusEnum.isEnable(key.getStatus())) {
                continue;
            }
            if (StrUtil.isBlank(key.getApiKey())) {
                continue;
            }
            if (!supportsVisionCapability(key)) {
                continue;
            }
            String visionModelName = resolveVisionModelName(key, preferredKeyId, preferredChatModelName);
            if (StrUtil.isBlank(visionModelName) || isLikelyNoVisionModel(visionModelName)) {
                continue;
            }
            Map<String, Object> profile = toVisionProfileMap(key, visionModelName,
                    preferredTemp, preferredMaxTokens,
                    StrUtil.blankToDefault(key.getPlatform(), preferredPlatform));
            if (preferredKeyId != null && preferredKeyId.equals(key.getId())) {
                preferred.add(profile);
            } else {
                others.add(profile);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>(preferred.size() + others.size());
        out.addAll(preferred);
        out.addAll(others);
        return out;
    }

    /**
     * 识图模型解析顺序：密钥.visionModel → 密钥.chatModel →（默认对话密钥时）默认 CHAT 模型标识。
     * 禁止使用 imageModel / imageEditModel。
     */
    private static String resolveVisionModelName(AiApiKeyDO key, Long preferredKeyId,
                                                 String preferredChatModelName) {
        String vision = StrUtil.trim(key.getVisionModel());
        if (StrUtil.isNotBlank(vision) && !isLikelyNoVisionModel(vision) && !isImageGenerationModel(vision)) {
            return vision;
        }
        String chat = StrUtil.trim(key.getChatModel());
        if (StrUtil.isNotBlank(chat) && !isLikelyNoVisionModel(chat) && !isImageGenerationModel(chat)) {
            return chat;
        }
        if (preferredKeyId != null && preferredKeyId.equals(key.getId())
                && StrUtil.isNotBlank(preferredChatModelName)
                && !isLikelyNoVisionModel(preferredChatModelName)
                && !isImageGenerationModel(preferredChatModelName)) {
            return preferredChatModelName.trim();
        }
        return "";
    }

    private static boolean supportsVisionCapability(AiApiKeyDO key) {
        // 已配识图模型名（且不是生图模型）→ 直接纳入
        if (StrUtil.isNotBlank(key.getVisionModel())
                && !isLikelyNoVisionModel(key.getVisionModel())
                && !isImageGenerationModel(key.getVisionModel())) {
            return true;
        }
        String caps = StrUtil.blankToDefault(key.getCapabilities(), "").toLowerCase();
        if (StrUtil.isNotBlank(caps)) {
            return StrUtil.contains(caps, "vision");
        }
        // 未标注能力：chatModel 看起来带视觉也纳入（仍排除生图模型）
        return StrUtil.isNotBlank(key.getChatModel())
                && !isLikelyNoVisionModel(key.getChatModel())
                && !isImageGenerationModel(key.getChatModel());
    }

    private static Map<String, Object> toVisionProfileMap(AiApiKeyDO key, String visionModelName,
                                                          Double temperature, Integer maxTokens,
                                                          String platform) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("keyId", key.getId());
        out.put("keyName", StrUtil.blankToDefault(key.getName(), ""));
        out.put("platform", StrUtil.blankToDefault(platform, key.getPlatform()));
        out.put("model", visionModelName);
        out.put("chatModel", StrUtil.blankToDefault(key.getChatModel(), visionModelName));
        out.put("baseUrl", StrUtil.blankToDefault(key.getUrl(), ""));
        out.put("apiKey", StrUtil.blankToDefault(key.getApiKey(), ""));
        out.put("temperature", temperature != null ? temperature : 0.3);
        out.put("maxTokens", maxTokens != null ? maxTokens : 4096);
        out.put("gatewayType", StrUtil.blankToDefault(key.getGatewayType(), "openai_compatible"));
        out.put("capabilities", StrUtil.blankToDefault(key.getCapabilities(), "chat,vision"));
        out.put("preferResponsesApi", Integer.valueOf(1).equals(key.getPreferResponsesApi()));
        out.put("visionImageDetail", StrUtil.blankToDefault(key.getVisionImageDetail(), "high"));
        out.put("imageBodyStyle", StrUtil.blankToDefault(key.getImageBodyStyle(), ""));
        out.put("supportsAsync", Integer.valueOf(1).equals(key.getSupportsAsync()));
        out.put("imageModel", StrUtil.blankToDefault(key.getImageModel(), ""));
        out.put("imageEditModel", StrUtil.blankToDefault(key.getImageEditModel(),
                StrUtil.blankToDefault(key.getImageModel(), "")));
        out.put("extraConfig", StrUtil.blankToDefault(key.getExtraConfig(), ""));
        return out;
    }

    @Override
    public List<Map<String, Object>> classifyImages(XqRpaAiClassifyImagesReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        List<String> images = normalizeUrls(reqVO.getSourceImages());
        if (images.size() > 8) {
            images = new ArrayList<>(images.subList(0, 8));
        }
        log.info("[classifyImages] 服务端不下载原图，仅 URL 兜底分型 count={}", images.size());
        return classifyMock(images);
    }

    @Override
    public List<Map<String, Object>> generateImagePrompts(XqRpaAiImagePromptsReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        List<Map<String, Object>> metaList = reqVO.getImagesMeta() == null ? List.of() : reqVO.getImagesMeta();
        if (metaList.size() > 8) {
            metaList = metaList.subList(0, 8);
        }
        Map<String, Object> copy = reqVO.getCopyResult() == null ? Map.of() : reqVO.getCopyResult();
        Map<String, Object> imageRule = reqVO.getImageRule() == null ? Map.of() : reqVO.getImageRule();
        List<String> points = normalizePoints(copy.get("sellingPoints"));
        List<String> styleTags = normalizeHighlightTags(copy.get("highlightStyle"));
        String styleText = String.join("、", styleTags);
        String baseHint = str(imageRule.get("promptText"));
        String neg = str(imageRule.get("negativePrompt"));
        boolean mock = useMock(reqVO.getMock());

        List<Map<String, Object>> out = new ArrayList<>();
        List<Map<String, Object>> batch = new ArrayList<>();
        for (Map<String, Object> meta : metaList) {
            int idx = toInt(meta.get("index"), out.size());
            String t = StrUtil.blankToDefault(str(meta.get("imageType")), "other").toLowerCase();
            String marker = StrUtil.blankToDefault(str(meta.get("marker")), TYPE_MARKERS.getOrDefault(t, "其它参考图"));
            String nameLine = nameLineOf(t, idx);
            String point = points.isEmpty() ? "产品卖点" : points.get(idx % points.size());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", idx);
            row.put("imageType", t);
            row.put("imageUrl", meta.get("imageUrl"));
            row.put("marker", marker);
            row.put("bindIndexes", meta.get("bindIndexes") != null ? meta.get("bindIndexes") : List.of(idx));
            row.put("nameLine", nameLine);
            row.put("sellingPoint", point);
            out.add(row);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", idx);
            item.put("imageName", nameLine);
            item.put("imageType", t);
            item.put("marker", marker);
            item.put("sellingPoint", point);
            item.put("imageUrl", str(meta.get("imageUrl")));
            batch.add(item);
        }
        if (out.isEmpty()) {
            return out;
        }
        Map<Integer, String> promptByIndex = new LinkedHashMap<>();
        if (!mock) {
            String system = "你是电商生图提示词二创专家。文案已完成，本任务只写图片提示词。"
                    + "必须依据中文特征点(highlightStyle)、英文卖点、以及每张图的 imageType/imageLabel 标识做二次创作。"
                    + "禁止完全按原图临摹；可保留产品真实外形/材质/颜色，优化构图与场景干净度。"
                    + "只返回 JSON："
                    + "[{\"index\":0,\"imageType\":\"scene\",\"imageLabel\":\"图1-场景图\",\"promptText\":\"场景图\\n中文提示词\"}]。"
                    + "promptText 必须全部中文：第一行是图名（主图-卖点/尺寸图/细节图/场景图/包装图/参考图），"
                    + "后面用中文写构图、材质、光线、背景、要突出的特征点。禁止英文句子、禁止半截词。";
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("title", copy.get("title"));
            user.put("highlightStyle", copy.get("highlightStyle"));
            user.put("featurePoints", copy.get("featurePoints") != null ? copy.get("featurePoints") : copy.get("highlightStyle"));
            user.put("sellingPoints", points);
            user.put("creativeMode", "feature_based_rewrite");
            user.put("rulePrompt", StrUtil.maxLength(baseHint, 300));
            user.put("negativePrompt", StrUtil.maxLength(neg, 200));
            user.put("images", batch);
            JSONArray arr = extractJsonArray(chat(system, JSONUtil.toJsonStr(user), 2200));
            Map<Integer, JSONObject> byIndex = new LinkedHashMap<>();
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    if (item != null) {
                        byIndex.put(item.getInt("index", i), item);
                    }
                }
            }
            for (Map<String, Object> row : out) {
                int idx = toInt(row.get("index"), 0);
                JSONObject item = byIndex.get(idx);
                if (item != null && StrUtil.isNotBlank(item.getStr("imageType"))) {
                    String t = item.getStr("imageType").toLowerCase();
                    if (IMAGE_TYPE_SET.contains(t)) {
                        row.put("imageType", t);
                        row.put("marker", TYPE_MARKERS.getOrDefault(t, str(row.get("marker"))));
                        row.put("nameLine", nameLineOf(t, idx));
                    }
                }
                if (item != null) {
                    promptByIndex.put(idx, StrUtil.trim(item.getStr("promptText")));
                }
            }
        }
        for (Map<String, Object> row : out) {
            int idx = toInt(row.get("index"), 0);
            String nameLine = str(row.get("nameLine"));
            String t = str(row.get("imageType"));
            String marker = str(row.get("marker"));
            String point = str(row.get("sellingPoint"));
            String prompt = promptByIndex.get(idx);
            if (StrUtil.isBlank(prompt)) {
                prompt = nameLine + "\n电商实拍风，中文构图说明。"
                        + (StrUtil.isNotBlank(styleText) ? " 风格标签：" + styleText + "。" : "")
                        + marker + "。光线干净，产品居中，适合跨境主图。"
                        + (StrUtil.isNotBlank(baseHint) ? " 规则：" + StrUtil.maxLength(baseHint, 120) : "");
                if ("dimension".equals(t)) {
                    prompt += " 产品外形与尺寸标注同框，标注清晰可读。";
                }
            } else if (!prompt.startsWith(nameLine)) {
                prompt = nameLine + "\n" + prompt;
            }
            row.put("promptText", prompt);
            row.remove("nameLine");
            row.remove("sellingPoint");
        }
        return out;
    }

    @Override
    public Map<String, Object> generateImage(XqRpaAiGenerateImageReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        return doGenerateImage(reqVO);
    }

    @Override
    public Map<String, Object> generateImageForConsole(XqRpaAiGenerateImageReqVO reqVO) {
        // 管理端登录态：主 API 同步等待上游（过渡），单图改图预览用
        return doGenerateImage(reqVO);
    }

    private Map<String, Object> doGenerateImage(XqRpaAiGenerateImageReqVO reqVO) {
        String prompt = StrUtil.trim(reqVO == null ? null : reqVO.getPrompt());
        if (StrUtil.isBlank(prompt)) {
            throw exception(XQ_RPA_AI_FAIL, "提示词为空");
        }
        boolean lockProduct = reqVO.getLockProduct() == null || Boolean.TRUE.equals(reqVO.getLockProduct());
        List<Map<String, Object>> images = CollUtil.emptyIfNull(reqVO.getImages());
        RefImage ref = pickFirstRef(images);
        if (ref == null || (ref.bytes == null && StrUtil.isBlank(ref.url))) {
            throw exception(XQ_RPA_AI_FAIL, "请至少提供一张参考图（base64 或 url）");
        }
        String finalPrompt = lockProduct
                ? "【产品锁死】必须保持参考图中产品的外形、颜色、结构、比例、材质特征不变，禁止换成其它品类商品。\n\n" + prompt
                : prompt;

        AiApiKeyDO key = pickImageApiKey();
        if (key == null) {
            throw exception(XQ_RPA_AI_FAIL,
                    "未找到可用生图 API。请到【API 密钥】启用带 image_gen/image_edit 的密钥，并填写「生图模型」");
        }
        String model = StrUtil.blankToDefault(key.getImageEditModel(), key.getImageModel());
        long start = System.currentTimeMillis();
        try {
            // ToAPIs：参考图须其可访问的公网 URL（文档禁止 base64）；本地/内网 URL 会在
            // OpenAiCompatibleImageEditApi 内先 POST /v1/uploads/images 再 generations。
            // 其它网关：multipart /images/edits 吃 bytes。
            String referUrl = ref.url;
            byte[] referBytes = ref.bytes;
            if ((referBytes == null || referBytes.length == 0) && StrUtil.isNotBlank(referUrl)) {
                referBytes = downloadBytes(referUrl);
            }
            OpenAiCompatibleImageEditApi.EditResult edit = OpenAiCompatibleImageEditApi.edit(
                    key, model, finalPrompt, 2000, 2000, referUrl, referBytes);
            byte[] outBytes = null;
            String outUrl = StrUtil.blankToDefault(edit.getUrl(), "");
            if (StrUtil.isNotBlank(edit.getB64Json())) {
                outBytes = Base64.decode(edit.getB64Json());
            } else if (StrUtil.isNotBlank(outUrl)) {
                outBytes = downloadBytes(outUrl);
            }
            if (outBytes == null || outBytes.length == 0) {
                throw exception(XQ_RPA_AI_FAIL, "生图结果为空");
            }
            // 以实际产物字节为准（GPT Image 常回 PNG，勿沿用参考图 jpg 后缀）
            String mime = detectImageMime(outBytes, guessMime(ref.mimeType, outUrl));
            String ext = mimeToExt(mime);
            String fileName = "xq_rpa_" + StrUtil.blankToDefault(reqVO.getSku(), "img")
                    + "_" + System.currentTimeMillis() + "." + ext;
            String storedUrl = fileService.createFile(outBytes, fileName, "xq/rpa/gen", mime);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("url", storedUrl);
            data.put("mimeType", mime);
            data.put("extension", ext);
            data.put("sku", StrUtil.blankToDefault(reqVO.getSku(), ""));
            data.put("model", StrUtil.blankToDefault(edit.getModel(), model));
            data.put("costMs", System.currentTimeMillis() - start);
            data.put("promptUsed", finalPrompt);
            if (Boolean.TRUE.equals(reqVO.getReturnBase64())) {
                data.put("contentBase64", Base64.encode(outBytes));
            }
            log.info("[rpa-ai-generate-image] ok sku={} keyId={} costMs={} url={}",
                    reqVO.getSku(), key.getId(), data.get("costMs"), StrUtil.maxLength(storedUrl, 120));
            String usageCat = StrUtil.blankToDefault(reqVO.getUsageCategory(),
                    XqAiUsageLogService.CAT_IMAGE_GEN);
            if (!XqAiUsageLogService.CAT_IMAGE_REVISE.equals(usageCat)) {
                usageCat = XqAiUsageLogService.CAT_IMAGE_GEN;
            }
            aiUsageLogService.record(reqVO.getWorkOrderId(), usageCat,
                    StrUtil.blankToDefault(edit.getModel(), model),
                    0, 0, 1, null, "generate-image");
            return data;
        } catch (RuntimeException ex) {
            log.warn("[rpa-ai-generate-image] fail sku={} err={}", reqVO.getSku(), ex.getMessage());
            throw exception(XQ_RPA_AI_FAIL, StrUtil.blankToDefault(ex.getMessage(), "生图失败"));
        }
    }

    @Override
    public Map<String, Object> uploadImage(XqRpaAiUploadImageReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        byte[] bytes = decodeBase64Image(reqVO == null ? null : reqVO.getContentBase64());
        if (bytes == null || bytes.length == 0) {
            throw exception(XQ_RPA_AI_FAIL, "图片内容为空");
        }
        String mime = detectImageMime(bytes, StrUtil.blankToDefault(reqVO.getMimeType(), "image/jpeg"));
        String ext = mimeToExt(mime);
        String name = StrUtil.blankToDefault(reqVO.getFileName(), "");
        if (StrUtil.isBlank(name)) {
            name = "xq_rpa_" + StrUtil.blankToDefault(reqVO.getSku(), "img")
                    + "_" + System.currentTimeMillis() + "." + ext;
        } else {
            name = ensureExt(name, ext);
        }
        String url = fileService.createFile(bytes, name, "xq/rpa/upload", mime);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);
        data.put("objectKey", name);
        data.put("name", name);
        data.put("mimeType", mime);
        data.put("size", bytes.length);
        data.put("sku", StrUtil.blankToDefault(reqVO.getSku(), ""));
        data.put("storage", "infra");
        return data;
    }

    // ---------- helpers ----------

    private void assertToken(String token) {
        if (!StrUtil.equals(callbackToken, StrUtil.blankToDefault(token, ""))) {
            throw exception(XQ_RPA_TRIGGER_FAIL, "回调 token 无效");
        }
    }

    private AiApiKeyDO pickImageApiKey() {
        List<AiApiKeyDO> keys = CollUtil.emptyIfNull(aiApiKeyService.getApiKeyList());
        AiApiKeyDO fallback = null;
        for (AiApiKeyDO key : keys) {
            if (key == null || !CommonStatusEnum.ENABLE.getStatus().equals(key.getStatus())) {
                continue;
            }
            if (StrUtil.isBlank(key.getApiKey()) || StrUtil.isBlank(key.getUrl())) {
                continue;
            }
            String model = StrUtil.blankToDefault(key.getImageEditModel(), key.getImageModel());
            if (StrUtil.isBlank(model)) {
                continue;
            }
            String caps = StrUtil.blankToDefault(key.getCapabilities(), "").toLowerCase(Locale.ROOT);
            if (caps.contains("image_edit") || caps.contains("image_gen") || caps.contains("image")) {
                return key;
            }
            if (fallback == null) {
                fallback = key;
            }
        }
        return fallback;
    }

    private static RefImage pickFirstRef(List<Map<String, Object>> images) {
        for (Map<String, Object> item : images) {
            if (item == null) {
                continue;
            }
            RefImage ref = new RefImage();
            ref.fileName = StrUtil.blankToDefault(str(item.get("fileName")), str(item.get("name")));
            if (StrUtil.isBlank(ref.fileName)) {
                ref.fileName = "ref.jpg";
            }
            ref.mimeType = StrUtil.blankToDefault(str(item.get("mimeType")), str(item.get("mime")));
            if (StrUtil.isBlank(ref.mimeType)) {
                ref.mimeType = "image/jpeg";
            }
            ref.url = str(item.get("url"));
            String b64 = StrUtil.blankToDefault(str(item.get("contentBase64")),
                    StrUtil.blankToDefault(str(item.get("base64")), str(item.get("data"))));
            if (StrUtil.isNotBlank(b64)) {
                ref.bytes = decodeBase64Image(b64);
            }
            if (ref.bytes != null || StrUtil.startWithAny(ref.url, "http://", "https://")) {
                return ref;
            }
        }
        return null;
    }

    private static byte[] decodeBase64Image(String raw) {
        String text = StrUtil.blankToDefault(raw, "").trim();
        if (StrUtil.isBlank(text)) {
            return null;
        }
        int comma = text.indexOf("base64,");
        if (comma >= 0) {
            text = text.substring(comma + "base64,".length());
        }
        try {
            return Base64.decode(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private static byte[] downloadBytes(String url) {
        HttpResponse resp = HttpRequest.get(url).timeout(120_000).execute();
        try {
            if (resp.getStatus() >= 400) {
                throw new IllegalArgumentException("下载参考图失败 HTTP " + resp.getStatus());
            }
            return resp.bodyBytes();
        } finally {
            resp.close();
        }
    }

    private static String guessMime(String fallback, String url) {
        String u = StrUtil.blankToDefault(url, "").toLowerCase(Locale.ROOT);
        if (u.contains(".png")) {
            return "image/png";
        }
        if (u.contains(".webp")) {
            return "image/webp";
        }
        if (u.contains(".gif")) {
            return "image/gif";
        }
        if (u.contains(".jpg") || u.contains(".jpeg")) {
            return "image/jpeg";
        }
        return StrUtil.blankToDefault(fallback, "image/png");
    }

    /**
     * 按魔数识别图片 MIME，保证与 FileService「后缀 vs 内容」校验一致。
     */
    private static String detectImageMime(byte[] bytes, String fallback) {
        if (bytes != null && bytes.length >= 3) {
            // PNG
            if (bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return "image/png";
            }
            // JPEG
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            // GIF
            if (bytes.length >= 6
                    && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
                return "image/gif";
            }
            // WEBP: RIFF....WEBP
            if (bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
                return "image/webp";
            }
            // BMP
            if (bytes[0] == 'B' && bytes[1] == 'M') {
                return "image/bmp";
            }
        }
        String fb = StrUtil.blankToDefault(fallback, "").toLowerCase(Locale.ROOT);
        if (fb.contains("png") || fb.contains("jpeg") || fb.contains("jpg")
                || fb.contains("webp") || fb.contains("gif") || fb.contains("bmp")) {
            if (fb.contains("jpg") && !fb.contains("jpeg")) {
                return "image/jpeg";
            }
            return fb.startsWith("image/") ? fb : "image/" + fb;
        }
        return "image/png";
    }

    private static String mimeToExt(String mime) {
        String m = StrUtil.blankToDefault(mime, "").toLowerCase(Locale.ROOT);
        if (m.contains("png")) {
            return "png";
        }
        if (m.contains("webp")) {
            return "webp";
        }
        if (m.contains("gif")) {
            return "gif";
        }
        if (m.contains("bmp")) {
            return "bmp";
        }
        return "jpg";
    }

    /** 强制文件名后缀与真实 MIME 一致（替换错误后缀）。 */
    private static String ensureExt(String fileName, String ext) {
        String name = StrUtil.blankToDefault(fileName, "img");
        String e = StrUtil.blankToDefault(ext, "jpg").replace(".", "");
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        // 去掉路径残留
        name = name.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (StrUtil.isBlank(name)) {
            name = "img";
        }
        return name + "." + e;
    }

    private static final class RefImage {
        private byte[] bytes;
        private String url;
        private String mimeType;
        private String fileName;
    }

    private boolean useMock(Boolean reqMock) {
        return Boolean.TRUE.equals(reqMock) || mockWhenEmpty;
    }

    private String chat(String system, String user) {
        return chat(system, user, 1200);
    }

    private String chat(String system, String user, int maxTokens) {
        long start = System.currentTimeMillis();
        AiModelDO model = null;
        try {
            model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            return doChat(model, system, user, maxTokens, start);
        } catch (RuntimeException ex) {
            String detail = resolveAiErrorDetail(ex);
            // HTTP/2 被中转重置：清缓存后用强制 HTTP/1.1 的新客户端再试一次
            if (model != null && isHttp2ProtocolError(detail)) {
                try {
                    log.warn("[xq.rpa.ai] PROTOCOL_ERROR，重建 Chat 客户端后重试 modelId={}", model.getId());
                    aiModelService.evictChatModel(model.getId());
                    return doChat(model, system, user, maxTokens, start);
                } catch (RuntimeException retryEx) {
                    detail = resolveAiErrorDetail(retryEx);
                    log.warn("[xq.rpa.ai] retry fail {}ms: {}", System.currentTimeMillis() - start, detail);
                    if (retryEx.getClass().getName().contains("ServiceException")) {
                        throw retryEx;
                    }
                    throw exception(XQ_RPA_AI_FAIL, StrUtil.blankToDefault(detail, "未知错误"));
                }
            }
            log.warn("[xq.rpa.ai] fail {}ms: {}", System.currentTimeMillis() - start, detail);
            if (ex.getClass().getName().contains("ServiceException")) {
                throw ex;
            }
            throw exception(XQ_RPA_AI_FAIL, StrUtil.blankToDefault(detail, "未知错误"));
        }
    }

    private String doChat(AiModelDO model, String system, String user, int maxTokens, long start) {
        AiApiKeyDO key = aiApiKeyService.validateApiKey(model.getKeyId());
        // 与 vision-profile 一致：密钥可覆盖实际模型名
        String modelName = StrUtil.blankToDefault(key.getChatModel(), model.getModel());
        ChatModel chatModel = aiModelService.getChatModel(model.getId());
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        Double temperature = model.getTemperature() != null ? model.getTemperature() : 0.3;
        int cap = Math.max(256, maxTokens);
        // 文案/提示词走普通 Chat，不带 reasoningEffort，避免中转把请求路由到慢推理模型
        ChatOptions options = AiUtils.buildChatOptions(platform, modelName, temperature, cap);
        Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(user)), options);
        ChatResponse resp = chatModel.call(prompt);
        String content = resp.getResult() != null && resp.getResult().getOutput() != null
                ? resp.getResult().getOutput().getText() : null;
        accumulateChatUsage(modelName, resp);
        log.info("[xq.rpa.ai] model={} {}ms chars={}", modelName,
                System.currentTimeMillis() - start, content == null ? 0 : content.length());
        if (StrUtil.isBlank(content)) {
            throw exception(XQ_RPA_AI_FAIL, "空响应");
        }
        return content;
    }

    private void accumulateChatUsage(String modelName, ChatResponse resp) {
        UsageAcc acc = USAGE_ACC.get();
        if (acc == null) {
            return;
        }
        acc.calls++;
        if (StrUtil.isNotBlank(modelName)) {
            acc.model = modelName;
        }
        try {
            if (resp == null || resp.getMetadata() == null || resp.getMetadata().getUsage() == null) {
                return;
            }
            var usage = resp.getMetadata().getUsage();
            if (usage.getPromptTokens() != null) {
                acc.promptTokens += usage.getPromptTokens();
            }
            if (usage.getCompletionTokens() != null) {
                acc.completionTokens += usage.getCompletionTokens();
            }
        } catch (Exception ignored) {
            // 部分中转不回 usage
        }
    }

    private static boolean isHttp2ProtocolError(String detail) {
        return StrUtil.containsIgnoreCase(detail, "PROTOCOL_ERROR")
                || StrUtil.containsIgnoreCase(detail, "stream was reset");
    }

    /** 文案模型只需可见产品信息，去掉 visionKey/本地路径等无关字段 */
    private static Map<String, Object> slimObservations(Map<String, Object> observations) {
        if (observations == null || observations.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> slim = new LinkedHashMap<>();
        for (String key : List.of(
                "productType", "materials", "colors", "shape", "sizeHints",
                "accessories", "scenes", "styleTags", "imageNotes",
                "heroOnly", "heroIndex", "imageCount")) {
            if (observations.containsKey(key) && observations.get(key) != null) {
                slim.put(key, observations.get(key));
            }
        }
        return slim.isEmpty() ? observations : slim;
    }

    /** 明确无视觉：gpt-3.5 / 纯文本 instruct 等（与 AI 对话校验对齐） */
    private static boolean isLikelyNoVisionModel(String modelName) {
        String name = StrUtil.blankToDefault(modelName, "").toLowerCase();
        if (isImageGenerationModel(name)) {
            return true;
        }
        return name.contains("gpt-3.5")
                || name.contains("instruct")
                || name.contains("text-embedding")
                || name.equals("gpt-4")
                || name.startsWith("gpt-4-0314")
                || name.startsWith("gpt-4-0613");
    }

    /** 生图模型，禁止用于本机识图 Chat */
    private static boolean isImageGenerationModel(String modelName) {
        String name = StrUtil.blankToDefault(modelName, "").toLowerCase()
                .replace("openai/", "");
        return name.contains("gpt-image")
                || name.contains("dall-e")
                || name.contains("dalle")
                || name.contains("midjourney")
                || name.contains("stable-diffusion")
                || name.startsWith("sd-")
                || name.startsWith("sdxl")
                || name.contains("flux")
                || name.contains("imagen")
                || name.contains("image-edit");
    }

    /** 规范化分项提示词：空字段丢弃 */
    private static Map<String, Object> normalizeCopyPrompts(Map<String, Object> raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        String title = StrUtil.trim(str(raw.get("title")));
        if (StrUtil.isNotBlank(title)) {
            out.put("title", StrUtil.maxLength(title, 2000));
        }
        String description = StrUtil.trim(str(raw.get("description")));
        if (StrUtil.isNotBlank(description)) {
            out.put("description", StrUtil.maxLength(description, 4000));
        }
        Object featuresObj = raw.get("features");
        List<String> features = new ArrayList<>();
        if (featuresObj instanceof List<?> list) {
            for (Object item : list) {
                String s = StrUtil.trim(str(item));
                if (StrUtil.isNotBlank(s)) {
                    features.add(StrUtil.maxLength(s, 1000));
                } else {
                    features.add("");
                }
            }
        } else if (featuresObj instanceof Map<?, ?> map) {
            for (int i = 1; i <= 8; i++) {
                Object v = map.get("feature" + i);
                if (v == null) {
                    v = map.get(String.valueOf(i));
                }
                String s = StrUtil.trim(str(v));
                features.add(StrUtil.isNotBlank(s) ? StrUtil.maxLength(s, 1000) : "");
            }
        }
        if (features.stream().anyMatch(StrUtil::isNotBlank)) {
            out.put("features", features);
        }
        return out;
    }

    private static String buildRevisionFromCopyPrompts(Map<String, Object> copyPrompts) {
        StringBuilder sb = new StringBuilder();
        String title = str(copyPrompts.get("title"));
        if (StrUtil.isNotBlank(title)) {
            sb.append("【标题提示词】\n").append(title).append("\n\n");
        }
        Object featuresObj = copyPrompts.get("features");
        if (featuresObj instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                String f = StrUtil.trim(str(list.get(i)));
                if (StrUtil.isNotBlank(f)) {
                    sb.append("【卖点").append(i + 1).append("提示词】\n").append(f).append("\n\n");
                }
            }
        }
        String description = str(copyPrompts.get("description"));
        if (StrUtil.isNotBlank(description)) {
            sb.append("【长描述提示词】\n").append(description).append("\n");
        }
        return sb.toString().trim();
    }

    private static String resolveAiErrorDetail(Throwable error) {
        if (error == null) {
            return "";
        }
        String best = StrUtil.blankToDefault(error.getMessage(), "");
        Throwable cur = error;
        int depth = 0;
        while (cur != null && depth++ < 8) {
            String msg = StrUtil.blankToDefault(cur.getMessage(), "");
            if (StrUtil.isNotBlank(msg) && !StrUtil.equalsIgnoreCase(msg, "Request failed")) {
                best = msg;
            }
            cur = cur.getCause();
        }
        if (StrUtil.containsIgnoreCase(best, "timeout")
                || StrUtil.containsIgnoreCase(best, "InterruptedIOException")) {
            return "上游请求超时，请检查中转站连通性或换更快的模型。原始错误：" + best;
        }
        if (StrUtil.containsIgnoreCase(best, "PROTOCOL_ERROR")
                || StrUtil.containsIgnoreCase(best, "stream was reset")) {
            return "上游 HTTP/2 被重置（PROTOCOL_ERROR），多为中转站不兼容 h2。原始错误：" + best;
        }
        return best;
    }

    private static String clipAtSentence(String text, int maxLen) {
        String s = StrUtil.blankToDefault(text, "").trim();
        if (s.length() <= maxLen) {
            return s;
        }
        String cut = s.substring(0, maxLen);
        int p = Math.max(Math.max(cut.lastIndexOf('。'), cut.lastIndexOf('.')),
                Math.max(cut.lastIndexOf('！'), cut.lastIndexOf('?')));
        if (p >= maxLen / 2) {
            return cut.substring(0, p + 1).trim();
        }
        int sp = cut.lastIndexOf(' ');
        return (sp > maxLen / 2 ? cut.substring(0, sp) : cut).trim();
    }

    private static Map<String, Object> mockCopy(String sku, String title0, String original, CopyRule rule) {
        List<String> points = new ArrayList<>();
        for (int i = 0; i < rule.featureCount; i++) {
            points.add("Highlight " + (i + 1) + ": Durable design for everyday use");
        }
        String title = title0.length() > rule.titleMaxLen ? title0.substring(0, rule.titleMaxLen) : title0;
        String desc = (title0 + ". Optimized listing copy based on source content. SKU " + sku + ". "
                + StrUtil.maxLength(original, 400)).trim();
        if (desc.length() > rule.descriptionMaxLen) {
            desc = desc.substring(0, rule.descriptionMaxLen);
        }
        List<String> tags = List.of("简洁电商风", "材质特写", "日常使用场景");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("sellingPoints", points);
        out.put("description", desc);
        out.put("highlightStyle", tags);
        out.put("featurePoints", tags);
        return out;
    }

    private static List<Map<String, Object>> classifyMock(List<String> images) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String t = guessTypeFromUrl(i, images.get(i));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", i);
            row.put("imageUrl", images.get(i));
            row.put("imageType", t);
            row.put("imageLabel", "图" + (i + 1) + "-" + typeCn(t));
            row.put("marker", TYPE_MARKERS.get(t));
            row.put("bindIndexes", "dimension".equals(t) && i != 0 ? List.of(0, i) : List.of(i));
            out.add(row);
        }
        return out;
    }

    private static String typeCn(String t) {
        return switch (StrUtil.blankToDefault(t, "other")) {
            case "main" -> "主图";
            case "dimension" -> "尺寸图";
            case "detail" -> "细节图";
            case "scene" -> "场景图";
            case "package" -> "包装图";
            default -> "参考图";
        };
    }

    /** 禁止把第 2 张默认当尺寸图；尺寸图必须文件名像工程图，否则宁可不标 */
    private static String guessTypeFromUrl(int index, String url) {
        String u = StrUtil.blankToDefault(url, "").toLowerCase();
        if (u.contains("dimension") || u.contains("spec-sheet") || u.contains("sizechart")
                || u.contains("size-chart") || u.contains("/cad") || u.contains("line-drawing")
                || u.contains("measure") || u.contains("diagram")) {
            return "dimension";
        }
        if (u.contains("scene") || u.contains("lifestyle") || u.contains("kitchen")
                || u.contains("room") || u.contains("install")) {
            return "scene";
        }
        if (u.contains("detail") || u.contains("closeup") || u.contains("close-up") || u.contains("texture")) {
            return "detail";
        }
        if (u.contains("pack") || u.contains("box") || u.contains("kit")) {
            return "package";
        }
        return index == 0 ? "main" : "other";
    }

    private static String nameLineOf(String type, int idx) {
        return switch (StrUtil.blankToDefault(type, "other")) {
            case "main" -> "主图-卖点";
            case "dimension" -> "尺寸图";
            case "detail" -> "细节图";
            case "scene" -> "场景图";
            case "package" -> "包装图";
            default -> "参考图" + (idx + 1);
        };
    }

    private static CopyRule parseCopyRule(Map<String, Object> copyRule) {
        String configJson = "{}";
        if (copyRule != null) {
            Object cfg = copyRule.get("configJson");
            if (cfg != null) {
                configJson = String.valueOf(cfg);
            }
        }
        JSONObject cfg;
        try {
            cfg = JSONUtil.parseObj(StrUtil.blankToDefault(configJson, "{}"));
        } catch (Exception e) {
            cfg = new JSONObject();
        }
        JSONObject limits = cfg.getJSONObject("limits");
        if (limits == null) {
            limits = new JSONObject();
        }
        JSONObject hints = cfg.getJSONObject("hints");
        if (hints == null) {
            hints = new JSONObject();
        }
        JSONObject layout = cfg.getJSONObject("descriptionLayout");
        if (layout == null) {
            layout = new JSONObject();
        }
        JSONObject punct = cfg.getJSONObject("punctuation");
        if (punct == null) {
            punct = new JSONObject();
        }
        CopyRule rule = new CopyRule();
        rule.featureCount = cfg.getInt("defaultFeatureCount", 5);
        Object allowed = cfg.get("allowedFeatureCounts");
        if (allowed instanceof List<?> list && !list.isEmpty()) {
            rule.allowedFeatureCounts = list;
        } else {
            rule.allowedFeatureCounts = List.of(5, 6, 8, 9);
        }
        rule.generateTitle = cfg.getBool("generateTitle", true);
        rule.titleMaxLen = limits.getInt("titleMaxLen", 200);
        rule.titleMinLen = limits.getInt("titleMinLen", 0);
        rule.descriptionMaxLen = limits.getInt("descriptionMaxLen", 2000);
        rule.descriptionMinLen = limits.getInt("descriptionMinLen", 0);
        rule.featureMaxLen = limits.getInt("featureMaxLen", 200);
        rule.featureMinLen = limits.getInt("featureMinLen", 0);
        rule.featureTitleMaxLen = limits.getInt("featureTitleMaxLen", 60);
        rule.featureContentMaxLen = limits.getInt("featureContentMaxLen", 400);
        rule.titleHint = StrUtil.blankToDefault(hints.getStr("title"),
                StrUtil.blankToDefault(cfg.getStr("titleHint"), ""));
        rule.descriptionHint = StrUtil.blankToDefault(hints.getStr("description"),
                StrUtil.blankToDefault(cfg.getStr("descriptionHint"), ""));
        rule.featureHint = StrUtil.blankToDefault(hints.getStr("feature"),
                StrUtil.blankToDefault(cfg.getStr("featureHint"), ""));
        rule.descMode = StrUtil.blankToDefault(layout.getStr("mode"), "amazonLong");
        rule.specsHeading = StrUtil.blankToDefault(layout.getStr("specsHeading"), "Product Specifications:");
        rule.notesHeading = StrUtil.blankToDefault(layout.getStr("notesHeading"), "Important Notes:");
        rule.minSpecItems = layout.getInt("minSpecItems", 5);
        rule.maxSpecItems = layout.getInt("maxSpecItems", 12);
        rule.includeImportantNotes = layout.getBool("includeImportantNotes", false);
        rule.titleNoPunctuation = punct.getJSONObject("title") != null
                && punct.getJSONObject("title").getBool("noPunctuation", false);
        rule.featureNoPunctuation = punct.getJSONObject("feature") != null
                && punct.getJSONObject("feature").getBool("noPunctuation", false);
        JSONObject mode5 = cfg.getJSONObject("mode5");
        JSONObject mode8 = cfg.getJSONObject("mode8");
        if (mode5 == null) {
            mode5 = new JSONObject();
        }
        if (mode8 == null) {
            mode8 = new JSONObject();
        }
        // 按条数选择生效的卖点模式（与前端一致：>=7 用 mode8）
        JSONObject activeMode = rule.featureCount >= 7 ? mode8 : mode5;
        rule.featureSplitTitleContent = activeMode.getBool("splitTitleContent", rule.featureCount < 7);
        rule.featureRequireColon = activeMode.getBool("requireColon", rule.featureSplitTitleContent);
        rule.featureSeparator = StrUtil.blankToDefault(activeMode.getStr("separator"), ": ");
        rule.featureTitleHint = StrUtil.blankToDefault(activeMode.getStr("titleHint"), "Short Benefit Title");
        rule.raw = cfg;
        return rule;
    }

    /** 精简规则：三段均含 hint + layout.example + 字数/标点。 */
    private static Map<String, Object> buildCopyRulesPayload(CopyRule rule) {
        Map<String, Object> hints = new LinkedHashMap<>();
        hints.put("title", rule.titleHint);
        hints.put("description", rule.descriptionHint);
        hints.put("feature", rule.featureHint);

        Map<String, Object> titleLayout = new LinkedHashMap<>();
        titleLayout.put("noPunctuation", rule.titleNoPunctuation);
        titleLayout.put("minLen", rule.titleMinLen);
        titleLayout.put("maxLen", rule.titleMaxLen);
        titleLayout.put("example", rule.titleNoPunctuation
                ? "Touch Kitchen Faucet Brushed Nickel Pull Down Sprayer for Modern Kitchen Sink"
                : "Touch Kitchen Faucet with Pull-Down Sprayer, Brushed Nickel Finish.");

        Map<String, Object> descLayout = new LinkedHashMap<>();
        descLayout.put("mode", rule.descMode);
        descLayout.put("specsHeading", rule.specsHeading);
        descLayout.put("notesHeading", rule.notesHeading);
        descLayout.put("minSpecItems", rule.minSpecItems);
        descLayout.put("maxSpecItems", rule.maxSpecItems);
        descLayout.put("includeImportantNotes", rule.includeImportantNotes);
        if ("paragraph".equalsIgnoreCase(rule.descMode)) {
            descLayout.put("example",
                    "Upgrade your daily kitchen routine with this touch kitchen faucet. "
                            + "Enjoy effortless control and a clean modern look.");
        } else {
            descLayout.put("example",
                    "Upgrade your daily kitchen routine with this touch kitchen faucet...\n\n"
                            + rule.specsHeading + "\n"
                            + "- Finish: Brushed Nickel\n"
                            + "- Body Material: Metal");
        }

        Map<String, Object> featureLayout = new LinkedHashMap<>();
        featureLayout.put("count", rule.featureCount);
        featureLayout.put("splitTitleContent", rule.featureSplitTitleContent);
        featureLayout.put("requireColon", rule.featureRequireColon);
        featureLayout.put("separator", rule.featureSeparator);
        featureLayout.put("titleHint", rule.featureTitleHint);
        featureLayout.put("titleMaxLen", rule.featureTitleMaxLen);
        featureLayout.put("contentMaxLen", rule.featureContentMaxLen);
        featureLayout.put("noPunctuation", rule.featureNoPunctuation);
        if (rule.featureSplitTitleContent) {
            featureLayout.put("example",
                    "Easy Touch Control" + rule.featureSeparator
                            + "Flow starts and stops with a light tap for hands-free convenience.\n"
                            + "Durable Metal Body" + rule.featureSeparator
                            + "Built for daily kitchen use with a lasting brushed finish.");
        } else {
            featureLayout.put("example",
                    "Start and stop water flow with a light touch for convenient hands-free use.\n"
                            + "Durable metal body with brushed nickel finish suits modern kitchens.");
        }

        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("generateTitle", rule.generateTitle);
        rules.put("featureCount", rule.featureCount);
        rules.put("allowedFeatureCounts", rule.allowedFeatureCounts);
        rules.put("titleMinLen", rule.titleMinLen);
        rules.put("titleMaxLen", rule.titleMaxLen);
        rules.put("descriptionMinLen", rule.descriptionMinLen);
        rules.put("descriptionMaxLen", rule.descriptionMaxLen);
        rules.put("featureMinLen", rule.featureMinLen);
        rules.put("featureMaxLen", rule.featureMaxLen);
        rules.put("featureTitleMaxLen", rule.featureTitleMaxLen);
        rules.put("featureContentMaxLen", rule.featureContentMaxLen);
        rules.put("titleNoPunctuation", rule.titleNoPunctuation);
        rules.put("featureNoPunctuation", rule.featureNoPunctuation);
        rules.put("hints", hints);
        rules.put("titleLayout", titleLayout);
        rules.put("descriptionLayout", descLayout);
        rules.put("featureLayout", featureLayout);
        return rules;
    }

    private static List<String> normalizeUrls(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String u : raw) {
            String s = StrUtil.trim(u);
            if (StrUtil.isNotBlank(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private static List<String> pickSellingPoints(JSONObject parsed, String raw) {
        List<String> points = normalizePoints(parsed == null ? null : parsed.get("sellingPoints"));
        if (points.isEmpty() && parsed != null) {
            points = normalizePoints(parsed.get("bulletPoints"));
        }
        if (points.isEmpty() && parsed != null) {
            points = normalizePoints(parsed.get("bullets"));
        }
        if (points.isEmpty() && parsed != null) {
            points = normalizePoints(parsed.get("features"));
        }
        if (points.isEmpty() && parsed != null) {
            points = normalizePoints(parsed.get("selling_points"));
        }
        if (points.isEmpty()) {
            points = extractPointsFromRaw(raw);
        }
        return points;
    }

    /** 从半截/非标准 JSON 文本里捞卖点字符串 */
    private static List<String> extractPointsFromRaw(String raw) {
        List<String> out = new ArrayList<>();
        if (StrUtil.isBlank(raw)) {
            return out;
        }
        Matcher m = Pattern.compile(
                "\"(?:sellingPoints|bulletPoints|bullets|features|selling_points)\"\\s*:\\s*\\[([\\s\\S]*?)(?:]|\\z)",
                Pattern.CASE_INSENSITIVE).matcher(raw);
        if (!m.find()) {
            return out;
        }
        Matcher item = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(m.group(1));
        while (item.find()) {
            String s = item.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\")
                    .trim();
            if (StrUtil.isNotBlank(s) && !StrUtil.startWithIgnoreCase(s, "Key benefit")) {
                out.add(s);
            }
        }
        return out;
    }

    private static String firstStr(JSONObject obj, String... keys) {
        if (obj == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            String v = StrUtil.trim(obj.getStr(key));
            if (StrUtil.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

    private static String firstMapStr(Map<?, ?> map, String... keys) {
        if (map == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            Object v = map.get(key);
            if (v == null) {
                continue;
            }
            String s = StrUtil.trim(String.valueOf(v));
            if (StrUtil.isNotBlank(s) && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return "";
    }

    private static List<String> normalizePoints(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    String s = firstMapStr(map, "text", "content", "value", "title");
                    if (StrUtil.isBlank(s)) {
                        String t = firstMapStr(map, "title");
                        String c = firstMapStr(map, "content");
                        if (StrUtil.isNotBlank(t) && StrUtil.isNotBlank(c)) {
                            s = t + ": " + c;
                        }
                    }
                    if (StrUtil.isNotBlank(s)) {
                        out.add(s);
                    }
                    continue;
                }
                String s = StrUtil.trim(String.valueOf(o));
                if (StrUtil.isNotBlank(s) && !"null".equalsIgnoreCase(s)) {
                    out.add(s);
                }
            }
            return out;
        }
        String text = String.valueOf(raw).trim();
        if (text.startsWith("[")) {
            try {
                return normalizePoints(JSONUtil.parseArray(text));
            } catch (Exception ignored) {
                // fallthrough
            }
        }
        for (String part : text.split("[\\n•\\-]+")) {
            String s = part.trim();
            if (StrUtil.isNotBlank(s)) {
                out.add(s);
            }
        }
        return out;
    }

    /** 突出风格：中文短标签，不是一整句话 */
    private static List<String> normalizeHighlightTags(Object raw) {
        List<String> tags = new ArrayList<>();
        if (raw == null) {
            return tags;
        }
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                addHighlightTag(tags, String.valueOf(o));
            }
            return tags;
        }
        String text = String.valueOf(raw).trim();
        if (text.startsWith("[")) {
            try {
                return normalizeHighlightTags(JSONUtil.parseArray(text));
            } catch (Exception ignored) {
                // fallthrough
            }
        }
        String[] parts = text.split("[,，、;；|/\\n]+");
        if (parts.length >= 2) {
            for (String part : parts) {
                addHighlightTag(tags, part);
            }
            return tags;
        }
        // 一整句话：不直接当标签
        return tags;
    }

    private static void addHighlightTag(List<String> tags, String raw) {
        String s = StrUtil.trim(raw).replaceAll("[。.!！?？\"“”']", "");
        if (StrUtil.isBlank(s)) {
            return;
        }
        // 过长截断保留前 12 字，避免模型吐稍长标签时整项丢弃
        if (s.length() > 12) {
            s = s.substring(0, 12);
        }
        if (s.length() < 2) {
            return;
        }
        if (!tags.contains(s)) {
            tags.add(s);
        }
    }

    private static List<String> fallbackHighlightTags(List<String> points, String desc) {
        List<String> tags = new ArrayList<>();
        tags.add("电商实拍");
        tags.add("材质特写");
        tags.add("日常使用");
        if (CollUtil.isNotEmpty(points)) {
            addHighlightTag(tags, StrUtil.maxLength(points.get(0), 8));
        }
        if (StrUtil.containsIgnoreCase(desc, "ceramic") || StrUtil.contains(desc, "陶瓷")) {
            addHighlightTag(tags, "釉面陶瓷");
        }
        return tags;
    }

    private static List<String> padPoints(List<String> points, int n, int maxLen) {
        List<String> out = new ArrayList<>();
        for (String p : points) {
            if (out.size() >= n) {
                break;
            }
            String s = clipAtSentence(p, Math.max(maxLen, 80));
            // 过滤历史占位，避免再次写回工单
            if (StrUtil.isBlank(s) || StrUtil.startWithIgnoreCase(s, "Key benefit")) {
                continue;
            }
            out.add(s);
        }
        // 条数不足时用末条轻微变体补齐，不再写 Key benefit 假数据
        String seed = out.isEmpty() ? "" : out.get(out.size() - 1);
        while (out.size() < n && StrUtil.isNotBlank(seed)) {
            out.add(seed);
        }
        return out;
    }

    private static JSONObject extractJsonObject(String text) {
        if (StrUtil.isBlank(text)) {
            return new JSONObject();
        }
        String cleaned = stripMarkdownFence(text.trim());
        try {
            return JSONUtil.parseObj(cleaned);
        } catch (Exception ignored) {
            // try block
        }
        Matcher m = JSON_BLOCK.matcher(cleaned);
        if (m.find()) {
            try {
                return JSONUtil.parseObj(m.group());
            } catch (Exception ignored) {
                // ignore
            }
        }
        // 常见：max_tokens 截断导致 JSON 未闭合
        String repaired = repairTruncatedJson(cleaned);
        if (StrUtil.isNotBlank(repaired)) {
            try {
                return JSONUtil.parseObj(repaired);
            } catch (Exception ignored) {
                // ignore
            }
        }
        return new JSONObject();
    }

    private static String stripMarkdownFence(String text) {
        String s = StrUtil.blankToDefault(text, "").trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end >= 0) {
                s = s.substring(0, end);
            }
        }
        return s.trim();
    }

    /** 尽量补全被截断的 JSON 对象（仅用于抢救 title/sellingPoints） */
    private static String repairTruncatedJson(String text) {
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        String s = text.substring(start);
        StringBuilder out = new StringBuilder(s);
        boolean inString = false;
        boolean escape = false;
        int brace = 0;
        int bracket = 0;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                brace++;
            } else if (c == '}') {
                brace--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            }
        }
        if (inString) {
            out.append('"');
        }
        while (bracket > 0) {
            out.append(']');
            bracket--;
        }
        while (brace > 0) {
            out.append('}');
            brace--;
        }
        return out.toString();
    }

    private static JSONArray extractJsonArray(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return JSONUtil.parseArray(text.trim());
        } catch (Exception ignored) {
            // try block
        }
        Matcher m = Pattern.compile("\\[[\\s\\S]*]").matcher(text);
        if (m.find()) {
            try {
                return JSONUtil.parseArray(m.group());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static JSONObject findByIndex(JSONArray arr, int index) {
        for (Object o : arr) {
            if (o instanceof JSONObject obj && obj.getInt("index", -1) == index) {
                return obj;
            }
        }
        return new JSONObject();
    }

    private static List<Integer> toIntList(Object raw) {
        List<Integer> out = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return out;
        }
        for (Object o : list) {
            try {
                out.add(Integer.parseInt(String.valueOf(o)));
            } catch (Exception ignored) {
                // skip
            }
        }
        return out;
    }

    private static int toInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static class CopyRule {
        int featureCount = 5;
        Object allowedFeatureCounts = List.of(5, 6, 8, 9);
        boolean generateTitle = true;
        int titleMinLen = 0;
        int titleMaxLen = 200;
        int descriptionMinLen = 0;
        int descriptionMaxLen = 2000;
        int featureMinLen = 0;
        int featureMaxLen = 200;
        int featureTitleMaxLen = 60;
        int featureContentMaxLen = 400;
        String titleHint = "";
        String descriptionHint = "";
        String featureHint = "";
        String descMode = "amazonLong";
        String specsHeading = "Product Specifications:";
        String notesHeading = "Important Notes:";
        int minSpecItems = 5;
        int maxSpecItems = 12;
        boolean includeImportantNotes = false;
        boolean titleNoPunctuation = false;
        boolean featureNoPunctuation = false;
        boolean featureSplitTitleContent = true;
        boolean featureRequireColon = true;
        String featureSeparator = ": ";
        String featureTitleHint = "Short Benefit Title";
        JSONObject raw = new JSONObject();
    }

}
