package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
        if (!mock && observations.isEmpty()) {
            throw exception(XQ_RPA_AI_FAIL, "无实拍识图结果，拒绝空写");
        }

        if (mock) {
            return mockCopy(sku, title0, original, rule);
        }

        String system = "你是亚马逊/跨境电商文案优化助手。只输出一个完整 JSON，禁止截断、禁止 markdown。"
                + "字段：{\"title\":\"完整英文标题\",\"sellingPoints\":[\"完整英文句子\"],"
                + "\"description\":\"完整英文长描述\",\"highlightStyle\":[\"中文标签\"]}。"
                + "必须依据 imageObservations（实拍识图结果）和 originalCopy 写，禁止编造图上没有的材质、配件、尺寸、认证。"
                + "图上看不到的信息不要写进卖点和描述。"
                + "title、sellingPoints、description 必须是完整英文句子，以句号结尾。"
                + "sellingPoints 条数必须等于指定卖点数。"
                + "description 4到8句、不少于80个英文单词。"
                + "highlightStyle 必须是 4 到 8 个中文短标签（每项 2 到 8 字），来自实拍风格/材质/场景，给生图勾选。"
                + "禁止把 highlightStyle 写成一整句话，禁止英文标签。";
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("sku", sku);
        user.put("productTitle", title0);
        user.put("originalCopy", StrUtil.maxLength(original, 4000));
        user.put("imageObservations", observations);
        user.put("sourceImageCount", sourceImages.size());
        user.put("featureCount", rule.featureCount);
        user.put("rules", Map.of(
                "generateTitle", rule.generateTitle,
                "titleMaxLen", rule.titleMaxLen,
                "descriptionMaxLen", rule.descriptionMaxLen,
                "featureMaxLen", rule.featureMaxLen,
                "allowedFeatureCounts", rule.allowedFeatureCounts,
                "fullConfig", rule.raw
        ));
        user.put("imageCount", CollUtil.size(reqVO.getSourceImages()));

        String raw = chat(system, JSONUtil.toJsonStr(user), 4096);
        JSONObject parsed = extractJsonObject(raw);
        List<String> points = normalizePoints(parsed.get("sellingPoints"));
        if (points.isEmpty()) {
            points = normalizePoints(parsed.get("bulletPoints"));
        }
        points = padPoints(points, rule.featureCount, Math.max(rule.featureMaxLen, 400));
        String title = clipAtSentence(StrUtil.blankToDefault(parsed.getStr("title"), title0), rule.titleMaxLen);
        String desc = clipAtSentence(StrUtil.blankToDefault(parsed.getStr("description"), ""),
                Math.max(rule.descriptionMaxLen, 2000));
        List<String> highlightTags = normalizeHighlightTags(parsed.get("highlightStyle"));
        if (highlightTags.isEmpty()) {
            highlightTags = fallbackHighlightTags(points, desc);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("sellingPoints", points);
        out.put("description", desc);
        out.put("highlightStyle", highlightTags);
        return out;
    }

    @Override
    public Map<String, Object> getVisionProfile(String callbackToken) {
        assertToken(callbackToken);
        AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
        AiApiKeyDO key = aiApiKeyService.validateApiKey(model.getKeyId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("platform", model.getPlatform());
        out.put("model", model.getModel());
        out.put("baseUrl", StrUtil.blankToDefault(key.getUrl(), ""));
        out.put("apiKey", StrUtil.blankToDefault(key.getApiKey(), ""));
        out.put("temperature", model.getTemperature() != null ? model.getTemperature() : 0.3);
        out.put("maxTokens", model.getMaxTokens() != null ? model.getMaxTokens() : 4096);
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
            String system = "你是电商生图提示词专家。只根据 images[].imageType、卖点和已选中文风格标签写提示词。"
                    + "不要假设看过原图像素。只返回 JSON："
                    + "[{\"index\":0,\"imageType\":\"scene\",\"promptText\":\"场景图\\n中文提示词\"}]。"
                    + "promptText 必须全部中文：第一行是图名（主图-卖点/尺寸图/细节图/场景图/包装图/参考图），"
                    + "后面用中文写构图、材质、光线、背景、要突出的标签。禁止英文句子、禁止半截词。";
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("title", copy.get("title"));
            user.put("highlightStyle", copy.get("highlightStyle"));
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
        try {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel chatModel = aiModelService.getChatModel(model.getId());
            AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
            Double temperature = model.getTemperature() != null ? model.getTemperature() : 0.3;
            int cap = Math.max(256, maxTokens);
            ChatOptions options = AiUtils.buildChatOptions(platform, model.getModel(), temperature, cap);
            if (platform == AiPlatformEnum.OPENAI || platform == AiPlatformEnum.GROK) {
                options = org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .model(model.getModel())
                        .temperature(temperature)
                        .maxCompletionTokens(cap)
                        .reasoningEffort("low")
                        .build();
            }
            Prompt prompt = new Prompt(List.of(new SystemMessage(system), new UserMessage(user)), options);
            ChatResponse resp = chatModel.call(prompt);
            String content = resp.getResult() != null && resp.getResult().getOutput() != null
                    ? resp.getResult().getOutput().getText() : null;
            log.info("[xq.rpa.ai] model={} {}ms chars={}", model.getModel(),
                    System.currentTimeMillis() - start, content == null ? 0 : content.length());
            if (StrUtil.isBlank(content)) {
                throw exception(XQ_RPA_AI_FAIL, "空响应");
            }
            return content;
        } catch (RuntimeException ex) {
            log.warn("[xq.rpa.ai] fail {}ms: {}", System.currentTimeMillis() - start, ex.getMessage());
            if (ex.getClass().getName().contains("ServiceException")) {
                throw ex;
            }
            throw exception(XQ_RPA_AI_FAIL, StrUtil.blankToDefault(ex.getMessage(), "未知错误"));
        }
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
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("sellingPoints", points);
        out.put("description", desc);
        out.put("highlightStyle", List.of("简洁电商风", "材质特写", "日常使用场景"));
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
            row.put("marker", TYPE_MARKERS.get(t));
            row.put("bindIndexes", "dimension".equals(t) && i != 0 ? List.of(0, i) : List.of(i));
            out.add(row);
        }
        return out;
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
        if (StrUtil.isBlank(s) || s.length() > 12) {
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
            out.add(clipAtSentence(p, Math.max(maxLen, 80)));
        }
        while (out.size() < n) {
            out.add("Key benefit " + (out.size() + 1));
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
