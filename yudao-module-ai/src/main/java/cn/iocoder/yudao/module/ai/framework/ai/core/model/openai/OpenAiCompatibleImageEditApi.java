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
 * - ToAPIs：先 {@code POST /v1/uploads/images} 拿公网 URL，再
 * {@code POST /v1/images/generations} + {@code reference_images}；响应多为异步任务，需轮询。
 * - Aixoras / Hao / 官方兼容：{@code POST /v1/images/edits} multipart
 * <p>
 * 跑在主 API 异步线程（{@code @Async}），长等待勿再同步阻塞 HTTP 入口。
 * <p>
 * 文档：
 * <a href="https://docs.toapis.com/docs/cn/api-reference/uploads/images">上传图片</a>、
 * <a href="https://docs.toapis.com/docs/cn/api-reference/images/gpt-image-2/generation">图生图</a>
 */
@Slf4j
public final class OpenAiCompatibleImageEditApi {

    private static final int TIMEOUT_MS = 180_000;
    private static final int TOAPIS_POLL_INTERVAL_MS = 2_000;
    private static final int TOAPIS_POLL_MAX_MS = 300_000;

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
            String publicRef = ensureToapisReferenceUrl(baseUrl, token, referImageUrl, imageBytes);
            JSONObject req = new JSONObject();
            req.set("model", usedModel);
            req.set("prompt", prompt);
            req.set("n", 1);
            req.set("size", pixelSizeToAspectRatio(size));
            // gpt-image-2 / 2.5 文档：resolution + quality；缺省用 1k/high
            req.set("resolution", "1k");
            req.set("quality", "high");
            req.set("response_format", "url");
            JSONArray refs = new JSONArray();
            refs.add(publicRef);
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
            if (status >= 400) {
                String msg = pickErrorMessage(body, "ToAPIs 生图 HTTP " + status);
                throw new IllegalArgumentException(msg);
            }
            return parseToapisGenerationResult(baseUrl, token, body, usedModel);
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
            if (status >= 400) {
                String msg = pickErrorMessage(body, "图片编辑 HTTP " + status);
                throw new IllegalArgumentException(msg);
            }
            return parseResult(body, usedModel);
        }
    }

    /**
     * ToAPIs 参考图必须是其可拉取的公网 URL（不接 base64）。
     * 本地/内网 URL 或仅有字节时：走 {@code POST /v1/uploads/images}。
     */
    static String ensureToapisReferenceUrl(String baseUrl, String token,
                                           String referImageUrl, byte[] imageBytes) {
        String url = StrUtil.trim(referImageUrl);
        // 已在 ToAPIs 文件域：直接复用
        if (isToapisHostedUrl(url)) {
            return url;
        }
        byte[] bytes = imageBytes;
        if (bytes == null || bytes.length == 0) {
            if (StrUtil.isBlank(url)) {
                throw new IllegalArgumentException("ToAPIs 改图需要参考图（URL 或文件字节）");
            }
            // 无 bytes：仅当明确是公网非内网 URL 时直传，否则先下载再 uploads
            if (isPublicHttpUrl(url) && !isPrivateOrLocalUrl(url)) {
                try {
                    bytes = downloadBytes(url);
                } catch (Exception ex) {
                    log.warn("[toapis-ref] 下载失败，尝试直传 URL err={}", ex.getMessage());
                    return url;
                }
            } else {
                bytes = downloadBytes(url);
            }
        }
        // 有 bytes：一律 uploads，避免自建存储/内网地址 ToAPIs 拉不到
        return uploadToToapis(baseUrl, token, bytes, guessFileName(url));
    }

    private static String uploadToToapis(String baseUrl, String token, byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("ToAPIs 上传参考图失败：文件为空");
        }
        if (bytes.length > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("ToAPIs 参考图不能超过 10MB");
        }
        String name = StrUtil.blankToDefault(fileName, "reference.jpg");
        String uploadUrl = baseUrl + "/uploads/images";
        HttpResponse resp = HttpRequest.post(uploadUrl)
                .header("Authorization", "Bearer " + token)
                .form("file", bytes, name)
                .form("purpose", "generation")
                .timeout(TIMEOUT_MS)
                .execute();
        int status = resp.getStatus();
        String body = resp.body();
        resp.close();
        if (status >= 400) {
            throw new IllegalArgumentException(pickErrorMessage(body, "ToAPIs 上传参考图 HTTP " + status));
        }
        if (StrUtil.isBlank(body) || !JSONUtil.isTypeJSON(body)) {
            throw new IllegalArgumentException("ToAPIs 上传参考图响应非 JSON");
        }
        JSONObject json = JSONUtil.parseObj(body);
        if (json.containsKey("success") && !Boolean.TRUE.equals(json.getBool("success"))) {
            throw new IllegalArgumentException(StrUtil.blankToDefault(json.getStr("message"), "ToAPIs 上传失败"));
        }
        JSONObject data = json.getJSONObject("data");
        String out = data != null ? data.getStr("url") : json.getStr("url");
        if (StrUtil.isBlank(out)) {
            throw new IllegalArgumentException("ToAPIs 上传未返回 url：" + StrUtil.maxLength(body, 200));
        }
        log.info("[toapis-upload] ok url={}", StrUtil.maxLength(out, 160));
        return out;
    }

    private static EditResult parseToapisGenerationResult(String baseUrl, String token,
                                                          String body, String usedModel) {
        if (StrUtil.isBlank(body) || !JSONUtil.isTypeJSON(body)) {
            throw new IllegalArgumentException("ToAPIs 生图响应非 JSON：" + StrUtil.maxLength(body, 120));
        }
        JSONObject json = JSONUtil.parseObj(body);
        // 同步形态：直接带 data/images
        if (json.getJSONArray("data") != null || json.getJSONArray("images") != null) {
            return parseResult(body, usedModel);
        }
        JSONObject nested = json.getJSONObject("result");
        if (nested != null && (nested.getJSONArray("data") != null || nested.getJSONArray("images") != null)) {
            return parseResult(nested.toString(), usedModel);
        }
        String taskId = StrUtil.blankToDefault(json.getStr("id"), json.getStr("task_id"));
        String status = StrUtil.blankToDefault(json.getStr("status"), "").toLowerCase(Locale.ROOT);
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("ToAPIs 生图响应缺少 data/id：" + StrUtil.maxLength(body, 200));
        }
        // queued / in_progress / 无 status 都进入轮询
        if ("failed".equals(status) || "error".equals(status)) {
            throw new IllegalArgumentException(pickErrorMessage(body, "ToAPIs 生图任务失败"));
        }
        if ("completed".equals(status) || "success".equals(status)) {
            JSONObject result = json.getJSONObject("result");
            if (result != null) {
                return parseResult(result.toString(), usedModel);
            }
        }
        return pollToapisTask(baseUrl, token, taskId, usedModel);
    }

    private static EditResult pollToapisTask(String baseUrl, String token, String taskId, String usedModel) {
        String pollUrl = baseUrl + "/images/generations/" + taskId;
        long deadline = System.currentTimeMillis() + TOAPIS_POLL_MAX_MS;
        String lastBody = "";
        while (System.currentTimeMillis() < deadline) {
            HttpResponse resp = HttpRequest.get(pollUrl)
                    .header("Authorization", "Bearer " + token)
                    .timeout(TIMEOUT_MS)
                    .execute();
            int status = resp.getStatus();
            lastBody = resp.body();
            resp.close();
            if (status >= 400) {
                throw new IllegalArgumentException(pickErrorMessage(lastBody, "ToAPIs 轮询 HTTP " + status));
            }
            if (StrUtil.isBlank(lastBody) || !JSONUtil.isTypeJSON(lastBody)) {
                sleepQuiet(TOAPIS_POLL_INTERVAL_MS);
                continue;
            }
            JSONObject json = JSONUtil.parseObj(lastBody);
            String st = StrUtil.blankToDefault(json.getStr("status"), "").toLowerCase(Locale.ROOT);
            if ("failed".equals(st) || "error".equals(st)) {
                JSONObject err = json.getJSONObject("error");
                String msg = err != null ? err.getStr("message") : json.getStr("fail_reason");
                throw new IllegalArgumentException(StrUtil.blankToDefault(msg, "ToAPIs 生图失败"));
            }
            if ("completed".equals(st) || "success".equals(st)) {
                JSONObject result = json.getJSONObject("result");
                if (result != null) {
                    return parseResult(result.toString(), usedModel);
                }
                // 偶发直接把 data 挂在根上
                if (json.getJSONArray("data") != null) {
                    return parseResult(lastBody, usedModel);
                }
            }
            // 同步结果提前返回
            if (json.getJSONArray("data") != null || json.getJSONArray("images") != null) {
                return parseResult(lastBody, usedModel);
            }
            sleepQuiet(TOAPIS_POLL_INTERVAL_MS);
        }
        throw new IllegalArgumentException("ToAPIs 生图超时未完成 taskId=" + taskId
                + " last=" + StrUtil.maxLength(lastBody, 160));
    }

    private static void sleepQuiet(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("ToAPIs 轮询被中断");
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

    private static boolean isToapisHostedUrl(String url) {
        String u = StrUtil.blankToDefault(url, "").toLowerCase(Locale.ROOT);
        return u.contains("toapis.com") || u.contains("toapis.cn") || u.contains("files.toapis");
    }

    private static boolean isPublicHttpUrl(String url) {
        String u = StrUtil.blankToDefault(url, "").toLowerCase(Locale.ROOT);
        return u.startsWith("http://") || u.startsWith("https://");
    }

    private static boolean isPrivateOrLocalUrl(String url) {
        String u = StrUtil.blankToDefault(url, "").toLowerCase(Locale.ROOT);
        return u.contains("localhost")
                || u.contains("127.0.0.1")
                || u.contains("0.0.0.0")
                || u.contains("://192.168.")
                || u.contains("://10.")
                || u.contains("://172.16.")
                || u.contains("://172.17.")
                || u.contains("://172.18.")
                || u.contains("://172.19.")
                || u.contains("://172.2")
                || u.contains("://172.3")
                || u.contains("[::1]");
    }

    private static String guessFileName(String url) {
        String u = StrUtil.blankToDefault(url, "");
        int q = u.indexOf('?');
        if (q > 0) {
            u = u.substring(0, q);
        }
        int slash = u.lastIndexOf('/');
        if (slash >= 0 && slash < u.length() - 1) {
            String name = u.substring(slash + 1);
            if (name.contains(".")) {
                return name;
            }
        }
        return "reference.jpg";
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
        String url = first != null ? first.getStr("url") : null;
        String b64 = first != null ? first.getStr("b64_json") : null;
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
        // 兼容用户填 https://toapis.cn 或 https://toapis.cn/v1
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
