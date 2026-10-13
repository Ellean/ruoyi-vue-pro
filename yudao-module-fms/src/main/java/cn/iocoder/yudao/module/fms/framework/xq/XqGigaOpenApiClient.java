package cn.iocoder.yudao.module.fms.framework.xq;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 大健云仓（GigaB2B）OpenAPI 客户端。跑在主 API 进程；仅按需单 SKU 拉取，勿批量长阻塞。
 */
@Slf4j
@Component
public class XqGigaOpenApiClient {

    public static final String PATH_PRODUCT_SKUS = "/b2b-overseas-api/v1/buyer/product/skus/v1";
    public static final String PATH_PRODUCT_DETAIL = "/b2b-overseas-api/v1/buyer/product/detailInfo/v1";
    public static final String PATH_PRODUCT_PRICE = "/b2b-overseas-api/v1/buyer/product/price/v1";
    public static final String PATH_INVENTORY_QUANTITY = "/b2b-overseas-api/v1/buyer/inventory/quantity/v2";

    private static final String PROD_BASE = "https://openapi.gigab2b.com";
    private static final String SANDBOX_BASE = "https://openapi-sandbox.gigab2b.com";
    private static final int TIMEOUT_MS = 60_000;

    @Data
    @Builder
    public static class RuntimeConfig {
        private String id;
        private String name;
        private String clientId;
        private String clientSecret;
        private boolean sandbox;
        private String baseUrl;
        /** pickup / dropship */
        private String priceRole;
    }

    public JSONObject post(String apiPath, Map<String, Object> body, RuntimeConfig config) {
        if (config == null || StrUtil.isBlank(config.getClientId()) || StrUtil.isBlank(config.getClientSecret())) {
            throw new IllegalStateException("GigaB2B 凭证未配置");
        }
        String base = resolveBaseUrl(config);
        String nonce = RandomUtil.randomString(10);
        long ts = System.currentTimeMillis();
        String sign = buildSign(config.getClientId(), config.getClientSecret(), apiPath, nonce, ts);
        String url = base + apiPath;
        try (HttpResponse resp = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("client-id", config.getClientId())
                .header("timestamp", String.valueOf(ts))
                .header("nonce", nonce)
                .header("sign", sign)
                .body(JSONUtil.toJsonStr(body == null ? Collections.emptyMap() : body))
                .timeout(TIMEOUT_MS)
                .execute()) {
            String respBody = StrUtil.blankToDefault(resp.body(), "{}");
            JSONObject json = JSONUtil.parseObj(respBody);
            if (!resp.isOk()) {
                String msg = StrUtil.blankToDefault(json.getStr("msg"),
                        "HTTP " + resp.getStatus());
                throw new IllegalStateException("GigaB2B 请求失败: " + msg);
            }
            return json;
        }
    }

    public List<JSONObject> postRows(String apiPath, List<String> skus, RuntimeConfig config) {
        JSONObject body = new JSONObject();
        body.set("skus", skus);
        JSONObject res = post(apiPath, body, config);
        if (!isOk(res)) {
            String msg = StrUtil.blankToDefault(res.getStr("msg"), res.getStr("subMsg"));
            log.warn("[giga] {} {} fail: {}", config.getName(), apiPath, msg);
            return Collections.emptyList();
        }
        return normalizeRows(res.get("data"));
    }

    public static boolean isOk(JSONObject res) {
        if (res == null) {
            return false;
        }
        Object code = res.get("code");
        if (code == null) {
            return true;
        }
        String c = String.valueOf(code);
        return "0".equals(c) || "200".equals(c) || "success".equalsIgnoreCase(c);
    }

    public static List<JSONObject> normalizeRows(Object data) {
        if (data == null) {
            return Collections.emptyList();
        }
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(item -> item instanceof JSONObject jo ? jo : JSONUtil.parseObj(item))
                    .collect(Collectors.toList());
        }
        if (data instanceof JSONObject obj) {
            Object rows = obj.get("list");
            if (rows == null) {
                rows = obj.get("rows");
            }
            if (rows == null) {
                rows = obj.get("records");
            }
            if (rows != null) {
                return normalizeRows(rows);
            }
            if (obj.containsKey("sku")) {
                return Collections.singletonList(obj);
            }
        }
        return Collections.emptyList();
    }

    public static String resolveBaseUrl(RuntimeConfig config) {
        if (StrUtil.isNotBlank(config.getBaseUrl())) {
            return StrUtil.removeSuffix(config.getBaseUrl().trim(), "/");
        }
        return config.isSandbox() ? SANDBOX_BASE : PROD_BASE;
    }

    static String buildSign(String clientId, String clientSecret, String apiPath,
                            String nonce, long timestamp) {
        String msg = clientId + "&" + apiPath + "&" + timestamp + "&" + nonce;
        String key = clientId + "&" + clientSecret + "&" + nonce;
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, key.getBytes(StandardCharsets.UTF_8));
        String hex = HexUtil.encodeHexStr(hmac.digest(msg.getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(hex.getBytes(StandardCharsets.UTF_8));
    }

}
