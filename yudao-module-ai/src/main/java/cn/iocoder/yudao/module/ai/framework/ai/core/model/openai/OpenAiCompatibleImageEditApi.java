package cn.iocoder.yudao.module.ai.framework.ai.core.model.openai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiImageGatewayModelUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * OpenAI 兼容中转：有参考图时的改图调用。
 * <p>
 * - ToAPIs：{@code POST /v1/images/generations} + {@code reference_images}
 * - Aixoras / Hao / 官方兼容：{@code POST /v1/images/edits} multipart
 * <p>
 * 跑在主 API 异步线程（{@code @Async}），长等待勿再同步阻塞 HTTP 入口。
 */
@Slf4j
public final class OpenAiCompatibleImageEditApi {

    private static final int TIMEOUT_MS = 180_000;

    private OpenAiCompatibleImageEditApi() {
    }

    public static EditResult edit(AiApiKeyDO apiKey, String model, String prompt,
                                  Integer width, Integer height,
                                  String referImageUrl, byte[] imageBytes) {
        String baseUrl = normalizeBaseUrl(apiKey != null ? apiKey.getUrl() : null);
        if (StrUtil.isBlank(baseUrl)) {
            throw new IllegalArgumentException("API 密钥未配置 Base URL");
        }
        String token = apiKey != null ? StrUtil.trim(apiKey.getApiKey()) : null;
        if (StrUtil.isBlank(token)) {
            throw new IllegalArgumentException("API 密钥为空");
        }
        String usedModel = AiImageGatewayModelUtils.normalizeImageModel(apiKey, model);
        String size = (width != null && height != null) ? (width + "x" + height) : "1024x1024";
        boolean toapis = isToapis(apiKey);
        String body;
        int status;
        if (toapis) {
            if (StrUtil.isBlank(referImageUrl)) {
                throw new IllegalArgumentException("ToAPIs 改图需要可访问的参考图 URL");
            }
            JSONObject req = new JSONObject();
            req.set("model", usedModel);
            req.set("prompt", prompt);
            req.set("n", 1);
            req.set("size", pixelSizeToAspectRatio(size));
            req.set("quality", "high");
            JSONArray refs = new JSONArray();
            refs.add(referImageUrl);
            req.set("reference_images", refs);
            String url = baseUrl + "/images/generations";
            HttpResponse resp = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .body(req.toString())
                    .timeout(TIMEOUT_MS)
                    .execute();
            status = resp.getStatus();
            body = resp.body();
            resp.close();
        } else {
            if (imageBytes == null || imageBytes.length == 0) {
                throw new IllegalArgumentException("参考图内容为空");
            }
            String url = baseUrl + "/images/edits";
            HttpRequest request = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + token)
                    .form("model", usedModel)
                    .form("prompt", prompt)
                    .form("size", size)
                    .form("n", "1")
                    .form("image", imageBytes, "reference.png")
                    .timeout(TIMEOUT_MS);
            if (AiImageGatewayModelUtils.isGptImageModel(usedModel)) {
                request.form("quality", "auto");
            } else {
                request.form("response_format", "b64_json");
            }
            HttpResponse resp = request.execute();
            status = resp.getStatus();
            body = resp.body();
            resp.close();
        }
        if (status >= 400) {
            String msg = pickErrorMessage(body, "图片编辑 HTTP " + status);
            throw new IllegalArgumentException(msg);
        }
        return parseResult(body, usedModel);
    }

    private static EditResult parseResult(String body, String usedModel) {
        if (StrUtil.isBlank(body) || !JSONUtil.isTypeJSON(body)) {
            throw new IllegalArgumentException("改图响应非 JSON：" + StrUtil.maxLength(body, 120));
        }
        JSONObject json = JSONUtil.parseObj(body);
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            // 部分中转把图放在 images
            data = json.getJSONArray("images");
        }
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("改图结果为空");
        }
        JSONObject first = data.getJSONObject(0);
        String url = first.getStr("url");
        String b64 = first.getStr("b64_json");
        if (StrUtil.isBlank(url) && StrUtil.isBlank(b64)) {
            // 偶发 { url: "", b64_json: "..." } 已处理；再兜底 data[0] 是字符串 URL
            String asStr = data.getStr(0);
            if (StrUtil.isNotBlank(asStr) && asStr.startsWith("http")) {
                url = asStr;
            }
        }
        if (StrUtil.isBlank(url) && StrUtil.isBlank(b64)) {
            throw new IllegalArgumentException("改图结果缺少 url / b64_json");
        }
        EditResult result = new EditResult();
        result.setModel(usedModel);
        result.setUrl(StrUtil.blankToDefault(url, null));
        result.setB64Json(StrUtil.blankToDefault(b64, null));
        return result;
    }

    private static String pickErrorMessage(String body, String fallback) {
        if (StrUtil.isBlank(body)) {
            return fallback;
        }
        try {
            if (JSONUtil.isTypeJSON(body)) {
                JSONObject json = JSONUtil.parseObj(body);
                Object err = json.get("error");
                if (err instanceof JSONObject errObj) {
                    String msg = errObj.getStr("message");
                    if (StrUtil.isNotBlank(msg)) {
                        return msg;
                    }
                }
                String message = json.getStr("message");
                if (StrUtil.isNotBlank(message)) {
                    return message;
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return fallback + "：" + StrUtil.maxLength(body, 200);
    }

    public static boolean isToapis(AiApiKeyDO apiKey) {
        if (apiKey == null) {
            return false;
        }
        if (StrUtil.equalsIgnoreCase(apiKey.getGatewayType(), "toapis")
                || StrUtil.equalsIgnoreCase(apiKey.getImageBodyStyle(), "toapis")) {
            return true;
        }
        String url = StrUtil.blankToDefault(apiKey.getUrl(), "").toLowerCase(Locale.ROOT);
        return url.contains("toapis");
    }

    /** 与 xq-erp pixelSizeToAspectRatio 对齐的简化版 */
    public static String pixelSizeToAspectRatio(String size) {
        String raw = StrUtil.blankToDefault(size, "1024x1024").toLowerCase(Locale.ROOT);
        if (raw.contains(":")) {
            return raw;
        }
        String[] parts = raw.split("[xX*]");
        if (parts.length != 2) {
            return "1:1";
        }
        try {
            int w = Integer.parseInt(parts[0].trim());
            int h = Integer.parseInt(parts[1].trim());
            if (w <= 0 || h <= 0) {
                return "1:1";
            }
            if (w == h) {
                return "1:1";
            }
            if (w > h) {
                return "3:2";
            }
            return "2:3";
        } catch (NumberFormatException ex) {
            return "1:1";
        }
    }

    public static String normalizeBaseUrl(String url) {
        String trimmed = StrUtil.removeSuffix(StrUtil.trim(url), "/");
        if (StrUtil.isEmpty(trimmed)) {
            return trimmed;
        }
        if (!StrUtil.endWithIgnoreCase(trimmed, "/v1")) {
            return trimmed + "/v1";
        }
        return trimmed;
    }

    @Data
    public static class EditResult {
        private String model;
        private String url;
        private String b64Json;
    }
}
