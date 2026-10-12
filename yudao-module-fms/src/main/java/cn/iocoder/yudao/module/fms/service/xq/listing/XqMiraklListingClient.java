package cn.iocoder.yudao.module.fms.service.xq.listing;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformCredentialDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_API_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_LISTING_CREDENTIAL_REQUIRED;

/**
 * Mirakl 产品导入（P41）最小路径：上传 CSV，返回 import_id。
 */
@Slf4j
@Component
public class XqMiraklListingClient {

    public Map<String, Object> publishProduct(
            XqListingPlatformCredentialDO cred,
            XqWorkOrderDO order,
            Map<String, String> listingValues) {
        if (cred == null || StrUtil.isBlank(cred.getApiKeyEnc()) || StrUtil.isBlank(cred.getBaseUrl())) {
            throw exception(XQ_LISTING_CREDENTIAL_REQUIRED);
        }
        if (StrUtil.isBlank(cred.getShopId())) {
            throw exception(XQ_LISTING_API_FAIL, "Mirakl 缺少 Shop ID，请在上架平台密钥中配置");
        }
        String baseUrl = cred.getBaseUrl().replaceAll("/+$", "");
        String apiKey = cred.getApiKeyEnc(); // 已解密
        String sku = StrUtil.blankToDefault(
                listingValues == null ? null : listingValues.get("sku"),
                StrUtil.blankToDefault(order.getExternalSku(), "SKU" + order.getId()));
        String title = StrUtil.blankToDefault(
                listingValues == null ? null : listingValues.get("title"),
                StrUtil.blankToDefault(order.getContentTitle(), order.getTitle()));
        String description = StrUtil.blankToDefault(
                listingValues == null ? null : listingValues.get("description"),
                StrUtil.blankToDefault(order.getContentSellingPoints(), ""));
        String image = firstImage(order, listingValues);

        // 通用最小列：shop_sku + title + description + image_1（具体类目可在 listingValues 覆盖）
        List<String> headers = new ArrayList<>(List.of("shop_sku", "title", "description", "image_1"));
        Map<String, String> row = new LinkedHashMap<>();
        row.put("shop_sku", sku);
        row.put("title", StrUtil.blankToDefault(title, sku));
        row.put("description", description);
        row.put("image_1", StrUtil.blankToDefault(image, ""));
        if (listingValues != null) {
            for (Map.Entry<String, String> e : listingValues.entrySet()) {
                if (StrUtil.isBlank(e.getKey()) || row.containsKey(e.getKey())) {
                    continue;
                }
                headers.add(e.getKey());
                row.put(e.getKey(), e.getValue());
            }
        }
        String csv = toCsv(headers, row);
        String url = baseUrl + "/api/products/imports?shop=" + cred.getShopId()
                + "&import_mode=NORMAL&operator_format=true";
        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("Authorization", apiKey)
                    .header("Accept", "application/json")
                    .form("file", csv.getBytes(StandardCharsets.UTF_8), "products_" + sku + ".csv")
                    .timeout(180_000)
                    .execute();
            String body = resp.body();
            if (!resp.isOk()) {
                throw exception(XQ_LISTING_API_FAIL,
                        "HTTP " + resp.getStatus() + " " + StrUtil.maxLength(body, 500));
            }
            JSONObject json = JSONUtil.parseObj(StrUtil.blankToDefault(body, "{}"));
            String importId = StrUtil.blankToDefault(
                    json.getStr("import_id"),
                    json.getStr("importId", json.getStr("product_import_id")));
            if (StrUtil.isBlank(importId)) {
                throw exception(XQ_LISTING_API_FAIL, "Mirakl 未返回 import_id：" + StrUtil.maxLength(body, 300));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("mode", "api");
            result.put("provider", "mirakl");
            result.put("importId", importId);
            result.put("shopId", cred.getShopId());
            result.put("sku", sku);
            result.put("message", "已提交 Mirakl 产品导入");
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[mirakl-publish] fail sku={} err={}", sku, ex.getMessage());
            throw exception(XQ_LISTING_API_FAIL, ex.getMessage());
        }
    }

    private static String firstImage(XqWorkOrderDO order, Map<String, String> values) {
        if (values != null) {
            for (String key : List.of("image_1", "main_image", "image_url")) {
                if (StrUtil.isNotBlank(values.get(key))) {
                    return values.get(key);
                }
            }
        }
        return StrUtil.trim(order.getGeneratedImageUrl());
    }

    private static String toCsv(List<String> headers, Map<String, String> row) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(";", headers)).append('\n');
        List<String> cells = new ArrayList<>();
        for (String h : headers) {
            cells.add(escape(row.getOrDefault(h, "")));
        }
        sb.append(String.join(";", cells)).append('\n');
        return sb.toString();
    }

    private static String escape(String raw) {
        String v = StrUtil.blankToDefault(raw, "");
        if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

}
