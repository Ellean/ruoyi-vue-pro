package cn.iocoder.yudao.module.fms.service.xq.listing;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformCredentialDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_API_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_CREDENTIAL_REQUIRED;

/**
 * Wayfair GraphQL 产品提交最小路径（supplier part addition）。
 */
@Slf4j
@Component
public class XqWayfairListingClient {

    private static final String AUTH_URL = "https://sso.auth.wayfair.com/oauth/token";
    private static final String API_PROD = "https://api.wayfair.com/v1/graphql";
    private static final String API_SANDBOX = "https://api.wayfair.io/v1/graphql";

    public Map<String, Object> publishProduct(
            XqListingPlatformCredentialDO cred,
            XqWorkOrderDO order,
            Map<String, String> listingValues) {
        if (cred == null || StrUtil.isBlank(cred.getClientId()) || StrUtil.isBlank(cred.getClientSecretEnc())) {
            throw exception(XQ_LISTING_CREDENTIAL_REQUIRED);
        }
        if (StrUtil.isBlank(cred.getSupplierId())) {
            throw exception(XQ_LISTING_API_FAIL, "Wayfair 缺少 supplierId，请在上架平台密钥中配置");
        }
        String sku = StrUtil.blankToDefault(
                listingValues == null ? null : listingValues.get("sku"),
                StrUtil.blankToDefault(order.getExternalSku(), "SKU" + order.getId()));
        String title = StrUtil.blankToDefault(
                listingValues == null ? null : listingValues.get("title"),
                StrUtil.blankToDefault(order.getContentTitle(), order.getTitle()));
        String token = fetchAccessToken(cred.getClientId(), cred.getClientSecretEnc(), cred.getEnv());
        String apiUrl = isSandbox(cred.getEnv()) ? API_SANDBOX : API_PROD;

        // 最小 mutation：按旧系统 productAddition 思路提交 supplierPartNumber + productName
        String mutation = """
                mutation($input: ProductAdditionInput!) {
                  productAddition(productAddition: $input) {
                    requestId
                    status
                  }
                }
                """;
        JSONObject input = new JSONObject();
        Long supplierIdNum = tryParseLong(cred.getSupplierId());
        if (supplierIdNum != null) {
            input.set("supplierId", supplierIdNum);
        } else {
            input.set("supplierId", cred.getSupplierId());
        }
        input.set("supplierPartNumber", sku);
        input.set("productName", StrUtil.blankToDefault(title, sku));
        if (listingValues != null && StrUtil.isNotBlank(listingValues.get("classId"))) {
            Long classId = tryParseLong(listingValues.get("classId"));
            if (classId != null) {
                input.set("classId", classId);
            }
        }

        JSONObject variables = new JSONObject();
        variables.set("input", input);
        JSONObject body = new JSONObject();
        body.set("query", mutation);
        body.set("variables", variables);

        try {
            HttpResponse resp = HttpRequest.post(apiUrl)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(body.toString())
                    .timeout(120_000)
                    .execute();
            String respBody = resp.body();
            if (!resp.isOk()) {
                throw exception(XQ_LISTING_API_FAIL,
                        "HTTP " + resp.getStatus() + " " + StrUtil.maxLength(respBody, 500));
            }
            JSONObject json = JSONUtil.parseObj(StrUtil.blankToDefault(respBody, "{}"));
            if (json.containsKey("errors")) {
                JSONArray errors = json.getJSONArray("errors");
                String msg = errors == null || errors.isEmpty()
                        ? respBody
                        : String.valueOf(errors.get(0));
                throw exception(XQ_LISTING_API_FAIL, StrUtil.maxLength(msg, 500));
            }
            JSONObject data = json.getJSONObject("data");
            JSONObject addition = data == null ? null : data.getJSONObject("productAddition");
            String requestId = addition == null ? "" : StrUtil.blankToDefault(
                    addition.getStr("requestId"), addition.getStr("request_id"));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("mode", "api");
            result.put("provider", "wayfair");
            result.put("requestId", requestId);
            result.put("supplierId", cred.getSupplierId());
            result.put("sku", sku);
            result.put("message", StrUtil.isBlank(requestId)
                    ? "已提交 Wayfair（未返回 requestId，请到后台核对）"
                    : "已提交 Wayfair 产品新增");
            result.put("rawStatus", addition == null ? null : addition.getStr("status"));
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[wayfair-publish] fail sku={} err={}", sku, ex.getMessage());
            throw exception(XQ_LISTING_API_FAIL, ex.getMessage());
        }
    }

    private String fetchAccessToken(String clientId, String clientSecret, String env) {
        try {
            HttpResponse resp = HttpRequest.post(AUTH_URL)
                    .form("grant_type", "client_credentials")
                    .form("client_id", clientId)
                    .form("client_secret", clientSecret)
                    .form("audience", isSandbox(env)
                            ? "https://sandbox.api.wayfair.com/"
                            : "https://api.wayfair.com/")
                    .timeout(60_000)
                    .execute();
            String body = resp.body();
            if (!resp.isOk()) {
                throw exception(XQ_LISTING_API_FAIL,
                        "Wayfair 鉴权失败 HTTP " + resp.getStatus() + " " + StrUtil.maxLength(body, 300));
            }
            JSONObject json = JSONUtil.parseObj(body);
            String token = json.getStr("access_token");
            if (StrUtil.isBlank(token)) {
                throw exception(XQ_LISTING_API_FAIL, "Wayfair 未返回 access_token");
            }
            return token;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(XQ_LISTING_API_FAIL, "Wayfair 鉴权异常：" + ex.getMessage());
        }
    }

    private static boolean isSandbox(String env) {
        String e = StrUtil.blankToDefault(env, "prod").toLowerCase();
        return e.contains("sand") || e.contains("stage") || e.contains("test");
    }

    private static Long tryParseLong(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim().replaceAll("[^0-9-]", ""));
        } catch (Exception ex) {
            return null;
        }
    }

}
