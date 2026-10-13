package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaApiCredentialDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductListMeta;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaApiCredentialMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.framework.xq.XqGigaOpenApiClient;
import cn.iocoder.yudao.module.fms.framework.xq.XqSecretCrypto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_GIGA_ENSURE_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_GIGA_ENSURE_NO_CREDENTIAL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_PRODUCT_NOT_EXISTS;
import static cn.iocoder.yudao.module.fms.framework.xq.XqGigaOpenApiClient.PATH_INVENTORY_QUANTITY;
import static cn.iocoder.yudao.module.fms.framework.xq.XqGigaOpenApiClient.PATH_PRODUCT_DETAIL;
import static cn.iocoder.yudao.module.fms.framework.xq.XqGigaOpenApiClient.PATH_PRODUCT_PRICE;

@Slf4j
@Service
@Validated
public class XqGigaProductEnsureServiceImpl implements XqGigaProductEnsureService {

    @Resource
    private XqGigaProductMapper gigaProductMapper;
    @Resource
    private XqGigaApiCredentialMapper credentialMapper;
    @Resource
    private XqSecretCrypto secretCrypto;
    @Resource
    private XqGigaOpenApiClient gigaOpenApiClient;
    @Lazy
    @Resource
    private XqProductService productService;

    @Override
    public boolean needsEnsure(XqProductRespVO vo) {
        if (vo == null) {
            return true;
        }
        boolean thinDetail = isBlank(vo.getUpc())
                && isBlank(vo.getAttributesJson())
                && isBlank(vo.getCharacteristics())
                && isBlank(vo.getMainMaterial())
                && (vo.getImageCount() == null || vo.getImageCount() < 2)
                && CollUtil.isEmpty(vo.getImageUrls());
        boolean missingPrices = vo.getPickupShippingFee() == null
                || vo.getDropshipShippingFee() == null;
        return thinDetail || missingPrices;
    }

    @Override
    public XqProductRespVO ensureDetail(String listId) {
        XqGigaProductListMeta meta = gigaProductMapper.selectListMetaById(listId);
        if (meta == null || StrUtil.isBlank(meta.getSku())) {
            throw exception(XQ_PRODUCT_NOT_EXISTS);
        }
        List<XqGigaOpenApiClient.RuntimeConfig> configs = loadEnabledConfigs();
        if (configs.isEmpty()) {
            throw exception(XQ_GIGA_ENSURE_NO_CREDENTIAL);
        }

        String sku = meta.getSku().trim();
        List<String> skus = Collections.singletonList(sku);
        JSONObject bestDetail = null;
        JSONObject bestInventory = null;
        List<PriceSource> priceSources = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (XqGigaOpenApiClient.RuntimeConfig config : configs) {
            try {
                List<JSONObject> details = gigaOpenApiClient.postRows(PATH_PRODUCT_DETAIL, skus, config);
                List<JSONObject> prices = gigaOpenApiClient.postRows(PATH_PRODUCT_PRICE, skus, config);
                List<JSONObject> inventories = gigaOpenApiClient.postRows(PATH_INVENTORY_QUANTITY, skus, config);

                JSONObject detail = findBySku(details, sku);
                JSONObject price = findBySku(prices, sku);
                JSONObject inventory = findBySku(inventories, sku);

                if (detail != null && richerDetail(detail, bestDetail)) {
                    bestDetail = detail;
                }
                if (inventory != null && bestInventory == null) {
                    bestInventory = inventory;
                }
                if (price != null) {
                    priceSources.add(toPriceSource(config, price));
                }
                if (detail == null && price == null && inventory == null) {
                    errors.add((config.getName() != null ? config.getName() : config.getId())
                            + "：无返回");
                }
            } catch (Exception ex) {
                String name = StrUtil.blankToDefault(config.getName(), config.getId());
                log.warn("[giga-ensure] sku={} config={} fail: {}", sku, name, ex.getMessage());
                errors.add(name + "：" + StrUtil.maxLength(ex.getMessage(), 120));
            }
        }

        if (bestDetail == null && priceSources.isEmpty()) {
            throw exception(XQ_GIGA_ENSURE_FAIL,
                    errors.isEmpty() ? "全部凭证均无数据" : String.join("; ", errors));
        }

        JSONObject mergedPrice = mergePriceSources(sku, priceSources);
        String userId = StrUtil.blankToDefault(meta.getUserId(), "xq-ensure");
        String gigaConfigId = StrUtil.blankToDefault(meta.getGigaConfigId(),
                configs.get(0).getId());
        String syncBatchNo = "GE" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        String productName = firstNonBlank(
                bestDetail != null ? bestDetail.getStr("productName") : null,
                meta.getProductName());
        Boolean skuAvailable = null;
        if (mergedPrice != null && mergedPrice.containsKey("skuAvailable")) {
            skuAvailable = mergedPrice.getBool("skuAvailable");
        } else if (bestDetail != null && bestDetail.containsKey("skuAvailable")) {
            skuAvailable = bestDetail.getBool("skuAvailable");
        }

        Integer sellerInv = null;
        Integer buyerInv = null;
        if (bestInventory != null) {
            JSONObject seller = bestInventory.getJSONObject("sellerInventoryInfo");
            JSONObject buyer = bestInventory.getJSONObject("buyerInventoryInfo");
            if (seller != null) {
                sellerInv = seller.getInt("sellerAvailableInventory");
            }
            if (buyer != null) {
                buyerInv = buyer.getInt("totalBuyerAvailableInventory");
            }
        }

        // 只写一行合并快照（含 priceSources + 自提/代发运费），避免多行互相抢最优
        gigaProductMapper.upsertSnapshot(
                IdUtil.getSnowflakeNextIdStr(),
                userId,
                gigaConfigId,
                sku,
                productName,
                skuAvailable,
                mergedPrice != null ? mergedPrice.getStr("currency") : null,
                decimalOrNull(mergedPrice, "price"),
                decimalOrNull(mergedPrice, "exclusivePrice"),
                decimalOrNull(mergedPrice, "discountedPrice"),
                decimalOrNull(mergedPrice, "shippingFee"),
                sellerInv,
                buyerInv,
                bestDetail != null ? bestDetail.toString() : null,
                mergedPrice != null ? mergedPrice.toString() : null,
                bestInventory != null ? bestInventory.toString() : null,
                syncBatchNo,
                now);

        return productService.getProduct(listId);
    }

    private List<XqGigaOpenApiClient.RuntimeConfig> loadEnabledConfigs() {
        List<XqGigaApiCredentialDO> rows = credentialMapper.selectList(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<XqGigaApiCredentialDO>()
                        .eq(XqGigaApiCredentialDO::getEnabled, true)
                        .orderByAsc(XqGigaApiCredentialDO::getVendorCode)
                        .orderByAsc(XqGigaApiCredentialDO::getPriceRole)
                        .orderByDesc(XqGigaApiCredentialDO::getIsDefault));
        List<XqGigaOpenApiClient.RuntimeConfig> out = new ArrayList<>();
        for (XqGigaApiCredentialDO row : rows) {
            String secret = secretCrypto.decrypt(row.getClientSecretEnc());
            if (StrUtil.isBlank(row.getClientId()) || StrUtil.isBlank(secret)) {
                continue;
            }
            out.add(XqGigaOpenApiClient.RuntimeConfig.builder()
                    .id(String.valueOf(row.getId()))
                    .name(row.getName())
                    .clientId(row.getClientId().trim())
                    .clientSecret(secret.trim())
                    .sandbox(Boolean.TRUE.equals(row.getSandbox()))
                    .baseUrl(row.getBaseUrl())
                    .priceRole(normalizeRole(row.getPriceRole(), row.getName()))
                    .build());
        }
        return out;
    }

    private static String normalizeRole(String raw, String name) {
        String role = StrUtil.blankToDefault(raw, "").trim().toLowerCase(Locale.ROOT);
        if (XqGigaApiCredentialDO.PRICE_ROLE_PICKUP.equals(role)
                || XqGigaApiCredentialDO.PRICE_ROLE_DROPSHIP.equals(role)) {
            return role;
        }
        String n = StrUtil.blankToDefault(name, "");
        if (n.contains("自提")) {
            return XqGigaApiCredentialDO.PRICE_ROLE_PICKUP;
        }
        if (n.contains("代发")) {
            return XqGigaApiCredentialDO.PRICE_ROLE_DROPSHIP;
        }
        return "auto";
    }

    private static JSONObject findBySku(List<JSONObject> rows, String sku) {
        if (CollUtil.isEmpty(rows)) {
            return null;
        }
        for (JSONObject row : rows) {
            if (row != null && sku.equalsIgnoreCase(StrUtil.trim(row.getStr("sku")))) {
                return row;
            }
        }
        return null;
    }

    private static boolean richerDetail(JSONObject candidate, JSONObject current) {
        if (candidate == null) {
            return false;
        }
        if (current == null) {
            return true;
        }
        return detailScore(candidate) > detailScore(current);
    }

    private static int detailScore(JSONObject detail) {
        int score = 0;
        JSONArray images = detail.getJSONArray("imageUrls");
        if (images != null) {
            score += images.size() * 10;
        }
        if (StrUtil.isNotBlank(detail.getStr("mainImageUrl"))) {
            score += 5;
        }
        if (StrUtil.isNotBlank(detail.getStr("upc"))) {
            score += 3;
        }
        if (detail.get("attributes") != null) {
            score += 3;
        }
        if (StrUtil.isNotBlank(detail.getStr("description"))
                || StrUtil.isNotBlank(detail.getStr("characteristics"))) {
            score += 2;
        }
        return score;
    }

    private static PriceSource toPriceSource(XqGigaOpenApiClient.RuntimeConfig config, JSONObject row) {
        BigDecimal shippingFee = firstNonNull(
                toDecimal(row.get("shippingFeePaid")),
                toDecimal(row.get("shippingFee")),
                toDecimal(row.get("shippingFeeFree")));
        PriceSource src = new PriceSource();
        src.configId = config.getId();
        src.name = config.getName();
        src.priceRole = config.getPriceRole();
        src.currency = row.getStr("currency");
        src.price = toDecimal(row.get("price"));
        src.exclusivePrice = toDecimal(row.get("exclusivePrice"));
        src.discountedPrice = toDecimal(row.get("discountedPrice"));
        src.shippingFee = shippingFee;
        src.shippingFeePaid = toDecimal(row.get("shippingFeePaid"));
        if (src.shippingFeePaid == null) {
            src.shippingFeePaid = toDecimal(row.get("shippingFee"));
        }
        src.shippingFeeFree = toDecimal(row.get("shippingFeeFree"));
        src.skuAvailable = row.getBool("skuAvailable");
        src.raw = row;
        return src;
    }

    /**
     * 多凭证价格合并：自提 / 代发角色分别取运费，货价优先自提凭证。
     */
    private static JSONObject mergePriceSources(String sku, List<PriceSource> sources) {
        if (CollUtil.isEmpty(sources)) {
            return null;
        }
        List<PriceSource> pickup = new ArrayList<>();
        List<PriceSource> dropship = new ArrayList<>();
        for (PriceSource s : sources) {
            if (XqGigaApiCredentialDO.PRICE_ROLE_PICKUP.equals(s.priceRole)) {
                pickup.add(s);
            } else if (XqGigaApiCredentialDO.PRICE_ROLE_DROPSHIP.equals(s.priceRole)) {
                dropship.add(s);
            }
        }
        boolean useRole = !pickup.isEmpty() || !dropship.isEmpty();

        JSONObject merged = new JSONObject();
        merged.set("sku", sku);
        JSONArray priceSourcesJson = new JSONArray();
        for (PriceSource s : sources) {
            JSONObject item = new JSONObject();
            item.set("id", s.configId);
            item.set("name", s.name);
            item.set("priceRole", s.priceRole);
            item.set("currency", s.currency);
            item.set("price", s.price);
            item.set("exclusivePrice", s.exclusivePrice);
            item.set("discountedPrice", s.discountedPrice);
            item.set("shippingFee", s.shippingFee);
            item.set("shippingFeePaid", s.shippingFeePaid);
            item.set("shippingFeeFree", s.shippingFeeFree);
            item.set("skuAvailable", s.skuAvailable);
            priceSourcesJson.add(item);
        }
        merged.set("priceSources", priceSourcesJson);
        merged.set("_multiConfigMerged", true);

        if (useRole) {
            PriceSource pickupSrc = pickup.isEmpty() ? sources.get(0) : pickup.get(0);
            PriceSource dropshipSrc = dropship.isEmpty() ? null : dropship.get(0);
            BigDecimal goodsPrice = pickGoodsPrice(pickupSrc, sources);
            BigDecimal pickupShipping = XqGigaApiCredentialDO.PRICE_ROLE_PICKUP.equals(pickupSrc.priceRole)
                    ? BigDecimal.ZERO
                    : firstNonNull(pickupSrc.shippingFeeFree, pickupSrc.shippingFee);
            BigDecimal dropshipShipping = dropshipSrc != null
                    ? firstNonNull(dropshipSrc.shippingFeePaid, dropshipSrc.shippingFee)
                    : null;

            merged.set("_mergeMode", "role");
            merged.set("currency", firstNonBlank(pickupSrc.currency,
                    dropshipSrc != null ? dropshipSrc.currency : null));
            merged.set("price", goodsPrice);
            merged.set("exclusivePrice", positiveOrNull(firstNonNull(
                    pickupSrc.exclusivePrice,
                    dropshipSrc != null ? dropshipSrc.exclusivePrice : null)));
            merged.set("discountedPrice", positiveOrNull(firstNonNull(
                    pickupSrc.discountedPrice,
                    dropshipSrc != null ? dropshipSrc.discountedPrice : null)));
            merged.set("pickupPrice", goodsPrice);
            merged.set("pickupConfigName", pickupSrc.name);
            merged.set("pickupShippingFee", pickupShipping);
            merged.set("dropshipConfigName", dropshipSrc != null ? dropshipSrc.name : null);
            merged.set("dropshipShippingFee", dropshipShipping);
            merged.set("shippingFee", dropshipShipping);
            merged.set("shippingFeeFree", BigDecimal.ZERO);
            merged.set("shippingFeePaid", dropshipShipping);
            merged.set("skuAvailable", sources.stream().anyMatch(s -> Boolean.TRUE.equals(s.skuAvailable)));
            return merged;
        }

        // 无角色时：按运费 0=自提免费、>0=代发
        PriceSource free = sources.stream()
                .filter(s -> s.shippingFee != null && s.shippingFee.compareTo(BigDecimal.ZERO) <= 0)
                .findFirst().orElse(null);
        PriceSource paid = sources.stream()
                .filter(s -> s.shippingFee != null && s.shippingFee.compareTo(BigDecimal.ZERO) > 0)
                .findFirst().orElse(null);
        PriceSource any = sources.get(0);
        BigDecimal goods = pickGoodsPrice(any, sources);
        merged.set("_mergeMode", "shipping");
        merged.set("currency", any.currency);
        merged.set("price", goods);
        merged.set("exclusivePrice", any.exclusivePrice);
        merged.set("discountedPrice", any.discountedPrice);
        merged.set("pickupShippingFee", free != null ? BigDecimal.ZERO
                : (any.shippingFeeFree != null ? any.shippingFeeFree : null));
        merged.set("dropshipShippingFee", paid != null ? paid.shippingFee : any.shippingFeePaid);
        merged.set("shippingFee", paid != null ? paid.shippingFee : any.shippingFee);
        merged.set("shippingFeeFree", free != null ? BigDecimal.ZERO : any.shippingFeeFree);
        merged.set("shippingFeePaid", paid != null ? paid.shippingFee : any.shippingFeePaid);
        merged.set("skuAvailable", sources.stream().anyMatch(s -> Boolean.TRUE.equals(s.skuAvailable)));
        return merged;
    }

    private static BigDecimal pickGoodsPrice(PriceSource preferred, List<PriceSource> all) {
        BigDecimal p = positiveOrNull(firstNonNull(
                preferred != null ? preferred.price : null,
                preferred != null ? preferred.discountedPrice : null,
                preferred != null ? preferred.exclusivePrice : null));
        if (p != null) {
            return p;
        }
        for (PriceSource s : all) {
            BigDecimal n = positiveOrNull(firstNonNull(s.price, s.discountedPrice, s.exclusivePrice));
            if (n != null) {
                return n;
            }
        }
        return null;
    }

    private static BigDecimal decimalOrNull(JSONObject obj, String key) {
        if (obj == null) {
            return null;
        }
        return toDecimal(obj.get(key));
    }

    private static BigDecimal toDecimal(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal bd) {
            return bd;
        }
        if (raw instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static BigDecimal positiveOrNull(BigDecimal n) {
        return n != null && n.compareTo(BigDecimal.ZERO) > 0 ? n : null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StrUtil.isNotBlank(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String v) {
        if (StrUtil.isBlank(v)) {
            return true;
        }
        String t = v.trim();
        return "null".equalsIgnoreCase(t) || "[]".equals(t) || "{}".equals(t);
    }

    private static class PriceSource {
        private String configId;
        private String name;
        private String priceRole;
        private String currency;
        private BigDecimal price;
        private BigDecimal exclusivePrice;
        private BigDecimal discountedPrice;
        private BigDecimal shippingFee;
        private BigDecimal shippingFeePaid;
        private BigDecimal shippingFeeFree;
        private Boolean skuAvailable;
        private JSONObject raw;
    }

}
