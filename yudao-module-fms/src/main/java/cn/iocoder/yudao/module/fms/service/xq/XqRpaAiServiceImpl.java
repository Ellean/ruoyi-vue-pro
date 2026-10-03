package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;
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
 * 主 API：OpenAI 兼容 Chat Completions，供文案 RPA 分步调用。
 */
@Service
@Validated
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
    @Value("${xq.ai.base-url:}")
    private String aiBaseUrl;
    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String springOpenAiBaseUrl;
    @Value("${xq.ai.api-key:}")
    private String aiApiKey;
    @Value("${spring.ai.openai.api-key:}")
    private String springOpenAiApiKey;
    @Value("${xq.ai.model:gpt-4o-mini}")
    private String aiModel;
    @Value("${xq.ai.timeout-ms:180000}")
    private int aiTimeoutMs;
    @Value("${xq.ai.mock-when-empty:true}")
    private boolean mockWhenEmpty;

    @Override
    public Map<String, Object> generateCopy(XqRpaAiGenerateCopyReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        CopyRule rule = parseCopyRule(reqVO.getCopyRule());
        String sku = StrUtil.blankToDefault(reqVO.getSku(), "");
        String title0 = StrUtil.blankToDefault(reqVO.getTitle(), sku);
        String original = StrUtil.blankToDefault(reqVO.getOriginalCopy(), "");
        boolean mock = useMock(reqVO.getMock());

        if (mock) {
            return mockCopy(sku, title0, original, rule);
        }

        String system = "你是亚马逊/跨境电商文案优化助手。必须遵守用户给出的文案规则硬约束。"
                + "根据原文案与产品信息输出 JSON："
                + "{\"title\":\"...\",\"sellingPoints\":[\"...\"],\"description\":\"...\","
                + "\"highlightStyle\":\"突出内容风格说明（中文可）\"}。"
                + "sellingPoints 条数必须等于指定卖点数；突出产品核心卖点与差异化；"
                + "不要编造认证/品牌；标题与卖点用英文（除非规则要求中文）。";
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("sku", sku);
        user.put("productTitle", title0);
        user.put("originalCopy", StrUtil.maxLength(original, 8000));
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

        String raw = chat(system, JSONUtil.toJsonStr(user));
        JSONObject parsed = extractJsonObject(raw);
        List<String> points = normalizePoints(parsed.get("sellingPoints"));
        if (points.isEmpty()) {
            points = normalizePoints(parsed.get("bulletPoints"));
        }
        points = padPoints(points, rule.featureCount, rule.featureMaxLen);
        String title = StrUtil.blankToDefault(parsed.getStr("title"), title0);
        if (title.length() > rule.titleMaxLen) {
            title = title.substring(0, rule.titleMaxLen);
        }
        String desc = StrUtil.blankToDefault(parsed.getStr("description"), "");
        if (desc.length() > rule.descriptionMaxLen) {
            desc = desc.substring(0, rule.descriptionMaxLen);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("sellingPoints", points);
        out.put("description", desc);
        out.put("highlightStyle", StrUtil.blankToDefault(parsed.getStr("highlightStyle"), desc));
        return out;
    }

    @Override
    public List<Map<String, Object>> classifyImages(XqRpaAiClassifyImagesReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        List<String> images = normalizeUrls(reqVO.getSourceImages());
        if (images.isEmpty() || useMock(reqVO.getMock())) {
            return classifyMock(images);
        }
        String system = "你是跨境电商图片质检助手。根据图片 URL 顺序判断每张图类型。"
                + "类型只能是: main, dimension, detail, scene, package, other。"
                + "第 1 张通常是主图。尺寸图含测量标注。"
                + "只返回 JSON 数组: [{\"index\":0,\"imageType\":\"main\",\"marker\":\"...\",\"bindIndexes\":[0]}]"
                + "尺寸图 bindIndexes 必须同时包含产品原图下标(通常0)和尺寸图自身下标。";
        StringBuilder user = new StringBuilder("图片列表:\n");
        for (int i = 0; i < images.size(); i++) {
            user.append(i).append(". ").append(images.get(i)).append('\n');
        }
        String raw = chat(system, user.toString());
        JSONArray arr = extractJsonArray(raw);
        if (arr == null || arr.isEmpty()) {
            return classifyMock(images);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            JSONObject item = findByIndex(arr, i);
            String t = StrUtil.blankToDefault(item.getStr("imageType"), i == 0 ? "main" : "other").toLowerCase();
            if (!IMAGE_TYPE_SET.contains(t)) {
                t = "other";
            }
            List<Integer> binds = toIntList(item.get("bindIndexes"));
            if (binds.isEmpty()) {
                binds = "dimension".equals(t) && i != 0 ? List.of(0, i) : List.of(i);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", i);
            row.put("imageUrl", images.get(i));
            row.put("imageType", t);
            row.put("marker", StrUtil.blankToDefault(item.getStr("marker"), TYPE_MARKERS.get(t)));
            row.put("bindIndexes", binds);
            out.add(row);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> generateImagePrompts(XqRpaAiImagePromptsReqVO reqVO) {
        assertToken(reqVO == null ? null : reqVO.getCallbackToken());
        List<Map<String, Object>> metaList = reqVO.getImagesMeta() == null ? List.of() : reqVO.getImagesMeta();
        Map<String, Object> copy = reqVO.getCopyResult() == null ? Map.of() : reqVO.getCopyResult();
        Map<String, Object> imageRule = reqVO.getImageRule() == null ? Map.of() : reqVO.getImageRule();
        List<String> points = normalizePoints(copy.get("sellingPoints"));
        String baseHint = str(imageRule.get("promptText"));
        String neg = str(imageRule.get("negativePrompt"));
        boolean mock = useMock(reqVO.getMock());

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> meta : metaList) {
            int idx = toInt(meta.get("index"), out.size());
            String t = StrUtil.blankToDefault(str(meta.get("imageType")), "other").toLowerCase();
            String marker = StrUtil.blankToDefault(str(meta.get("marker")), TYPE_MARKERS.getOrDefault(t, "其它参考图"));
            String nameLine = switch (t) {
                case "main" -> "主图-卖点";
                case "dimension" -> "尺寸图";
                case "detail" -> "细节图";
                case "scene" -> "场景图";
                case "package" -> "包装图";
                default -> "参考图" + (idx + 1);
            };
            String point = points.isEmpty() ? "product highlight" : points.get(idx % points.size());
            String prompt;
            if (mock) {
                prompt = nameLine + "\nType=" + t + "; Marker=" + marker + "; Focus=" + point
                        + ". Photorealistic ecommerce image, English on-image text only if needed. RuleHint="
                        + StrUtil.maxLength(baseHint, 180);
                if ("dimension".equals(t)) {
                    prompt += " Bind product photo + dimension annotation roles clearly.";
                }
            } else {
                String system = "你是电商生图提示词专家。输出纯文本提示词："
                        + "第一行=图片名称；其后为英文生图提示。"
                        + "必须体现图类型与标记；结合给定卖点；图上可读文字默认英文；"
                        + "尺寸图需说明产品外形参考图与尺寸标注图的角色。"
                        + "可参考内置兜底：产品保真、禁人类/宠物/侵权 Logo、画布约 2000x2000 1:1；"
                        + "用户规则提示词优先。";
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("imageName", nameLine);
                user.put("imageType", t);
                user.put("marker", marker);
                user.put("bindIndexes", meta.get("bindIndexes"));
                user.put("sellingPoint", point);
                user.put("title", copy.get("title"));
                user.put("highlightStyle", copy.get("highlightStyle"));
                user.put("rulePrompt", baseHint);
                user.put("negativePrompt", neg);
                prompt = StrUtil.trim(chat(system, JSONUtil.toJsonStr(user)));
                if (!prompt.startsWith(nameLine)) {
                    prompt = nameLine + "\n" + prompt;
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", idx);
            row.put("imageType", t);
            row.put("imageUrl", meta.get("imageUrl"));
            row.put("marker", marker);
            row.put("bindIndexes", meta.get("bindIndexes") != null ? meta.get("bindIndexes") : List.of(idx));
            row.put("promptText", prompt);
            out.add(row);
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
        if (Boolean.TRUE.equals(reqMock)) {
            return true;
        }
        String key = resolveApiKey();
        return mockWhenEmpty && (StrUtil.isBlank(key) || key.contains("xxxx"));
    }

    private String resolveApiKey() {
        return StrUtil.blankToDefault(aiApiKey, springOpenAiApiKey);
    }

    private String resolveBaseUrl() {
        String base = StrUtil.blankToDefault(aiBaseUrl, springOpenAiBaseUrl);
        base = StrUtil.removeSuffix(StrUtil.blankToDefault(base, "https://api.openai.com"), "/");
        if (base.endsWith("/chat/completions")) {
            return base.substring(0, base.length() - "/chat/completions".length());
        }
        if (!base.endsWith("/v1")) {
            base = base + "/v1";
        }
        return base;
    }

    private String chat(String system, String user) {
        String url = resolveBaseUrl() + "/chat/completions";
        String key = resolveApiKey();
        JSONObject body = new JSONObject();
        body.set("model", StrUtil.blankToDefault(aiModel, "gpt-4o-mini"));
        body.set("temperature", 0.4);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", system));
        messages.add(new JSONObject().set("role", "user").set("content", user));
        body.set("messages", messages);
        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + key.trim())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(body.toString())
                    .timeout(aiTimeoutMs)
                    .execute();
            if (!resp.isOk()) {
                throw exception(XQ_RPA_AI_FAIL, "HTTP " + resp.getStatus() + " " + StrUtil.maxLength(resp.body(), 300));
            }
            JSONObject data = JSONUtil.parseObj(resp.body());
            String content = data.getByPath("choices[0].message.content", String.class);
            if (StrUtil.isBlank(content)) {
                throw exception(XQ_RPA_AI_FAIL, "空响应");
            }
            return content;
        } catch (RuntimeException ex) {
            if (ex.getClass().getName().contains("ServiceException")) {
                throw ex;
            }
            throw exception(XQ_RPA_AI_FAIL, StrUtil.blankToDefault(ex.getMessage(), "未知错误"));
        }
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
        out.put("highlightStyle", "Clean modern ecommerce highlight: material, size fit, daily scene.");
        return out;
    }

    private static List<Map<String, Object>> classifyMock(List<String> images) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            String t;
            if (i == 0) {
                t = "main";
            } else if (i == 1) {
                t = "dimension";
            } else if (i == 2 || i == 3) {
                t = "detail";
            } else if (i == 4 || i == 5) {
                t = "scene";
            } else {
                t = "other";
            }
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

    private static List<String> padPoints(List<String> points, int n, int maxLen) {
        List<String> out = new ArrayList<>();
        for (String p : points) {
            if (out.size() >= n) {
                break;
            }
            out.add(p.length() > maxLen ? p.substring(0, maxLen) : p);
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
