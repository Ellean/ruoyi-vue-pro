package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaApiCredentialDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaApiCredentialMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.framework.xq.XqGigaOpenApiClient;
import cn.iocoder.yudao.module.fms.framework.xq.XqSecretCrypto;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.fms.framework.xq.XqGigaOpenApiClient.PATH_PRODUCT_SKUS;

/**
 * 定时选品列表同步（主 API 进程）。各凭证串行分开跑；只 INSERT，已有 SKU 跳过。
 */
@Slf4j
@Service
@Validated
public class XqGigaProductListSyncServiceImpl implements XqGigaProductListSyncService {

    private static final ZoneId GIGA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter GIGA_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 5000;
    private static final int MAX_PAGES = 50;
    private static final long PAGE_GAP_MS = 1100L;
    private static final long CREDENTIAL_GAP_MS = 2000L;
    private static final int DEFAULT_DAYS = 3;
    private static final int MAX_DAYS = 30;

    @Resource
    private XqGigaApiCredentialMapper credentialMapper;
    @Resource
    private XqGigaProductMapper gigaProductMapper;
    @Resource
    private XqSecretCrypto secretCrypto;
    @Resource
    private XqGigaOpenApiClient gigaOpenApiClient;

    @Override
    public String syncScheduledCredentials(int recentDays) {
        int days = recentDays > 0 ? Math.min(recentDays, MAX_DAYS) : DEFAULT_DAYS;
        List<XqGigaApiCredentialDO> creds = credentialMapper.selectScheduledSyncEnabled();
        if (CollUtil.isEmpty(creds)) {
            return "无启用定时同步的产品库凭证";
        }

        List<String> parts = new ArrayList<>();
        int totalInserted = 0;
        int totalSkipped = 0;
        int totalFetched = 0;
        int okCount = 0;

        for (int i = 0; i < creds.size(); i++) {
            XqGigaApiCredentialDO row = creds.get(i);
            try {
                CredResult r = syncOneCredential(row, days);
                totalInserted += r.inserted;
                totalSkipped += r.skipped;
                totalFetched += r.fetched;
                okCount += 1;
                parts.add(StrUtil.format("{}[{}/{}]: 拉取{} 新增{} 跳过{}",
                        StrUtil.blankToDefault(row.getName(), String.valueOf(row.getId())),
                        row.getPriceRole(),
                        StrUtil.blankToDefault(row.getVendorCode(), "-"),
                        r.fetched, r.inserted, r.skipped));
                log.info("[giga-list-sync] {}", parts.get(parts.size() - 1));
            } catch (Exception ex) {
                String msg = StrUtil.format("{}: FAIL {}",
                        StrUtil.blankToDefault(row.getName(), String.valueOf(row.getId())),
                        StrUtil.maxLength(ex.getMessage(), 160));
                parts.add(msg);
                log.warn("[giga-list-sync] {}", msg, ex);
            }
            if (i < creds.size() - 1) {
                sleepQuiet(CREDENTIAL_GAP_MS);
            }
        }

        return StrUtil.format("凭证{}套成功{}/{} 近{}天 拉取{} 新增{} 跳过{} | {}",
                creds.size(), okCount, creds.size(), days,
                totalFetched, totalInserted, totalSkipped,
                String.join("; ", parts));
    }

    private CredResult syncOneCredential(XqGigaApiCredentialDO row, int days) {
        String secret = secretCrypto.decrypt(row.getClientSecretEnc());
        if (StrUtil.isBlank(row.getClientId()) || StrUtil.isBlank(secret)) {
            throw new IllegalStateException("凭证密钥缺失");
        }
        // 强制只增：定时任务忽略 always_refresh，一律 skip_if_exists
        XqGigaOpenApiClient.RuntimeConfig config = XqGigaOpenApiClient.RuntimeConfig.builder()
                .id(String.valueOf(row.getId()))
                .name(row.getName())
                .clientId(row.getClientId().trim())
                .clientSecret(secret.trim())
                .sandbox(Boolean.TRUE.equals(row.getSandbox()))
                .baseUrl(row.getBaseUrl())
                .priceRole(row.getPriceRole())
                .build();

        Map<String, Object> timeWindow = buildRecentFavoritesWindow(days);
        List<JSONObject> rows = fetchAllListPages(config, timeWindow);
        CredResult result = new CredResult();
        result.fetched = rows.size();
        if (rows.isEmpty()) {
            return result;
        }

        // 按 SKU 去重（本批内）
        Map<String, JSONObject> bySku = new LinkedHashMap<>();
        for (JSONObject r : rows) {
            String sku = StrUtil.trim(r.getStr("sku"));
            if (StrUtil.isNotBlank(sku) && !bySku.containsKey(sku)) {
                bySku.put(sku, r);
            }
        }

        List<String> skuList = new ArrayList<>(bySku.keySet());
        Set<String> existing = new HashSet<>();
        for (int i = 0; i < skuList.size(); i += 500) {
            List<String> part = skuList.subList(i, Math.min(i + 500, skuList.size()));
            List<String> hit = gigaProductMapper.selectExistingListSkus(part);
            if (CollUtil.isNotEmpty(hit)) {
                existing.addAll(hit);
            }
        }

        String userId = StrUtil.blankToDefault(row.getVendorCode(), "xq-sched");
        String gigaConfigId = String.valueOf(row.getId());
        String syncBatchNo = "GL" + System.currentTimeMillis() + "-" + row.getId();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, JSONObject> e : bySku.entrySet()) {
            String sku = e.getKey();
            if (existing.contains(sku)) {
                result.skipped += 1;
                continue;
            }
            JSONObject src = e.getValue();
            String listId = IdUtil.getSnowflakeNextIdStr();
            String productName = StrUtil.blankToDefault(src.getStr("productName"),
                    src.getStr("product_name"));
            try {
                gigaProductMapper.insertProductList(
                        listId,
                        userId,
                        gigaConfigId,
                        sku,
                        productName,
                        StrUtil.blankToDefault(src.getStr("updateTime"), src.getStr("update_time")),
                        StrUtil.blankToDefault(src.getStr("firstArrivalDate"), src.getStr("first_arrival_date")),
                        StrUtil.blankToDefault(src.getStr("addedTime"), src.getStr("added_time")),
                        syncBatchNo,
                        now);
                gigaProductMapper.upsertFamilyIndexStub(sku, listId, productName, now);
                result.inserted += 1;
                existing.add(sku);
            } catch (Exception ex) {
                // 并发/唯一键冲突 → 视为已存在跳过
                log.debug("[giga-list-sync] skip insert sku={}: {}", sku, ex.getMessage());
                result.skipped += 1;
            }
        }
        return result;
    }

    private List<JSONObject> fetchAllListPages(XqGigaOpenApiClient.RuntimeConfig config,
                                               Map<String, Object> timeWindow) {
        List<JSONObject> all = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, Object> body = new HashMap<>(timeWindow);
            body.put("page", page);
            body.put("pageSize", PAGE_SIZE);
            body.put("sort", 2);
            // queryTimeType=2：收藏/加入时间窗口（近 N 天）
            JSONObject res = gigaOpenApiClient.post(PATH_PRODUCT_SKUS, body, config);
            if (!XqGigaOpenApiClient.isOk(res)) {
                String msg = StrUtil.blankToDefault(res.getStr("msg"), res.getStr("subMsg"));
                throw new IllegalStateException(StrUtil.blankToDefault(msg, "产品列表接口失败"));
            }
            List<JSONObject> rows = XqGigaOpenApiClient.normalizeRows(res.get("data"));
            all.addAll(rows);
            JSONObject data = res.getJSONObject("data");
            JSONObject pageInfo = data == null ? null : data.getJSONObject("pageInfo");
            Integer totalPage = pageInfo == null ? null : pageInfo.getInt("totalPage");
            if (rows.isEmpty() || rows.size() < PAGE_SIZE) {
                break;
            }
            if (totalPage != null && page >= totalPage) {
                break;
            }
            sleepQuiet(PAGE_GAP_MS);
        }
        return all;
    }

    /** 近 N 天收藏时间窗口（GMT+8，与大健 OpenAPI 一致） */
    static Map<String, Object> buildRecentFavoritesWindow(int days) {
        ZonedDateTime end = ZonedDateTime.now(GIGA_ZONE);
        ZonedDateTime start = end.minusDays(days).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Map<String, Object> map = new HashMap<>();
        map.put("queryTimeType", 2);
        map.put("startTime", GIGA_DT.format(start));
        map.put("endTime", GIGA_DT.format(end));
        return map;
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class CredResult {
        private int fetched;
        private int inserted;
        private int skipped;
    }

}
