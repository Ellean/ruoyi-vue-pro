package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;
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

    @Override
    public Map<String, Object> generateCopy(XqRpaAiGenerateCopyReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
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

        boolean regenerate = Boolean.TRUE.equals(reqVO.getRegenerate())
                || StrUtil.isNotBlank(reqVO.getRevisionPrompt())
                || (reqVO.getExistingCopy() != null && !reqVO.getExistingCopy().isEmpty());
        String revisionPrompt = StrUtil.blankToDefault(reqVO.getRevisionPrompt(), "");
        Map<String, Object> existingCopy = reqVO.getExistingCopy() == null
                ? Map.of() : reqVO.getExistingCopy();
        String system = "你是亚马逊/跨境电商文案优化助手。文案生成与图片提示词生成是两个独立概念："
                + "本任务只生成文案，不写生图提示词。"
                + "只输出一个完整 JSON，禁止截断、禁止 markdown。"
                + "字段：{\"title\":\"完整英文标题\",\"sellingPoints\":[\"完整英文句子\"],"
                + "\"description\":\"完整英文长描述\",\"highlightStyle\":[\"中文特征点\"]}。"
                + "输入优先：1) originalCopy 原文案；2) productTitle；3) 可选 imageObservations（可能为空，勿强依赖）。"
                + "必须依据原文案与标题写 title/sellingPoints/description；没有识图结果时不要编造图上看不到的材质、配件、尺寸、认证。"
                + "title、sellingPoints、description 必须是完整英文句子，以句号结尾。"
                + "sellingPoints 条数必须等于指定卖点数（5 或 8，按 rules）。"
                + "description 4到8句、不少于80个英文单词。"
                + "highlightStyle 是给后续生图用的中文特征点：4 到 8 个中文短标签（每项 2 到 8 字），"
                + "综合原文案与标题提炼材质/外形/场景/工艺特征；禁止英文、禁止整句。"
                + "不要输出 imagePrompts，不要按全套图逐张描述。";
        String target = StrUtil.blankToDefault(reqVO.getTarget(), "all").trim().toLowerCase();
        Integer featureIndex = reqVO.getFeatureIndex();
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
        user.put("revisionPrompt", StrUtil.maxLength(revisionPrompt, 2000));
        user.put("existingCopy", existingCopy);
        user.put("target", target);
        if (featureIndex != null) {
            user.put("featureIndex", featureIndex);
        }
        // 只传首图观察的关键字段，避免把整包 vision 元数据塞进文案模型
        user.put("imageObservations", slimObservations(observations));
        user.put("sourceImageCount", sourceImages.size());
        user.put("featureCount", rule.featureCount);
        // 不传 fullConfig：整份文案规则 JSON 很大，会拖慢中转与模型
        user.put("rules", Map.of(
                "generateTitle", rule.generateTitle,
                "titleMaxLen", rule.titleMaxLen,
                "descriptionMaxLen", rule.descriptionMaxLen,
                "featureMaxLen", rule.featureMaxLen,
                "allowedFeatureCounts", rule.allowedFeatureCounts
        ));
        user.put("imageCount", CollUtil.size(reqVO.getSourceImages()));

        // 8 条卖点 + 长描述约需 2.5k～3k；过小易截断成只有 title
        String raw = chat(system, JSONUtil.toJsonStr(user), 3072);
        JSONObject parsed = extractJsonObject(raw);
        List<String> points = normalizePoints(parsed.get("sellingPoints"));
        if (points.isEmpty()) {
            points = normalizePoints(parsed.get("bulletPoints"));
        }
        String title = clipAtSentence(StrUtil.blankToDefault(parsed.getStr("title"), title0), rule.titleMaxLen);
        String desc = clipAtSentence(StrUtil.blankToDefault(parsed.getStr("description"), ""),
                Math.max(rule.descriptionMaxLen, 2000));
        // 全量生成时禁止用 Key benefit 占位冒充成功（模型截断/空卖点）
        boolean fullGenerate = !regenerate || "all".equals(target);
        if (fullGenerate) {
            if (points.isEmpty()) {
                throw exception(XQ_RPA_AI_FAIL, "模型未返回卖点（可能被截断），请重试或换更快模型");
            }
            if (StrUtil.isBlank(desc)) {
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

    // ---------- helpers ----------

    private void assertToken(String token) {
        if (!StrUtil.equals(callbackToken, StrUtil.blankToDefault(token, ""))) {
            throw exception(XQ_RPA_TRIGGER_FAIL, "回调 token 无效");
        }
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
        log.info("[xq.rpa.ai] model={} {}ms chars={}", modelName,
                System.currentTimeMillis() - start, content == null ? 0 : content.length());
        if (StrUtil.isBlank(content)) {
            throw exception(XQ_RPA_AI_FAIL, "空响应");
        }
        return content;
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
        CopyRule rule = new CopyRule();
        rule.featureCount = cfg.getInt("defaultFeatureCount", 5);
        Object allowed = cfg.get("allowedFeatureCounts");
        if (allowed instanceof List<?> list && !list.isEmpty()) {
            rule.allowedFeatureCounts = list;
        } else {
            rule.allowedFeatureCounts = List.of(5, 8);
        }
        rule.generateTitle = cfg.getBool("generateTitle", true);
        rule.titleMaxLen = limits.getInt("titleMaxLen", 200);
        rule.descriptionMaxLen = limits.getInt("descriptionMaxLen", 2000);
        rule.featureMaxLen = limits.getInt("featureMaxLen", 200);
        rule.raw = cfg;
        return rule;
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

    private static List<String> normalizePoints(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                String s = StrUtil.trim(String.valueOf(o));
                if (StrUtil.isNotBlank(s)) {
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
        try {
            return JSONUtil.parseObj(text.trim());
        } catch (Exception ignored) {
            // try block
        }
        Matcher m = JSON_BLOCK.matcher(text);
        if (m.find()) {
            try {
                return JSONUtil.parseObj(m.group());
            } catch (Exception ignored) {
                // ignore
            }
        }
        return new JSONObject();
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
        Object allowedFeatureCounts = List.of(5, 8);
        boolean generateTitle = true;
        int titleMaxLen = 200;
        int descriptionMaxLen = 2000;
        int featureMaxLen = 200;
        JSONObject raw = new JSONObject();
    }

}
