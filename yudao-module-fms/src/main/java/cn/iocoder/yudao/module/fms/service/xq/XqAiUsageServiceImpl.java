package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage.XqAiUsageDashboardRespVO;
import cn.iocoder.yudao.module.fms.framework.xq.XqToapisUsageProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_TOAPIS_USAGE_FAIL;

@Service
@Slf4j
public class XqAiUsageServiceImpl implements XqAiUsageService {

    private static final long MAX_WINDOW_SEC = 31L * 24 * 3600;

    @Resource
    private XqToapisUsageProperties usageProperties;
    @Resource
    private AiApiKeyService aiApiKeyService;

    @Override
    public XqAiUsageDashboardRespVO getDashboard(Long startTimestamp, Long endTimestamp) {
        long end = endTimestamp != null && endTimestamp > 0
                ? endTimestamp
                : System.currentTimeMillis() / 1000;
        long start = startTimestamp != null && startTimestamp > 0
                ? startTimestamp
                : end - 7L * 24 * 3600;
        if (end <= start) {
            throw exception(XQ_TOAPIS_USAGE_FAIL, "时间窗口无效");
        }
        if (end - start > MAX_WINDOW_SEC) {
            throw exception(XQ_TOAPIS_USAGE_FAIL, "时间窗口最长 31 天");
        }

        XqAiUsageDashboardRespVO resp = new XqAiUsageDashboardRespVO();
        resp.setStartTimestamp(start);
        resp.setEndTimestamp(end);
        resp.setConfigured(isAccountConfigured());
        resp.setBalances(loadKeyBalances());

        if (!isAccountConfigured()) {
            resp.setHint("请配置 xq.toapis.usage.access-token 与 user-id（账户设置页「系统访问令牌」，非 sk- Key）。"
                    + "下方余额仍可按各 sk- 查询。文档：https://docs.toapis.cn/docs/cn/api-reference/account/token-usage");
            initEmptyCategories(resp);
            resp.setTotalCostUsd("0");
            resp.setTotalTokens(0L);
            resp.setTotalRequests(0L);
            return resp;
        }

        resp.setHint("数据来自 ToAPIs 计费日志；文案/生图/改图按模型名关键词归类，可在配置中调整。");

        List<JSONObject> summaryRows = fetchAllPages("/api/usage/tokens/summary", start, end, null);
        List<JSONObject> requestRows = fetchAllPages("/api/usage/tokens/requests", start, end, 100);

        Map<String, Agg> byCat = new LinkedHashMap<>();
        byCat.put("copy", new Agg());
        byCat.put("image_gen", new Agg());
        byCat.put("image_revise", new Agg());
        byCat.put("other", new Agg());

        BigDecimal totalCost = BigDecimal.ZERO;
        long totalTokens = 0;
        long totalRequests = 0;

        List<XqAiUsageDashboardRespVO.KeySummary> keys = new ArrayList<>();
        for (JSONObject row : summaryRows) {
            XqAiUsageDashboardRespVO.KeySummary ks = new XqAiUsageDashboardRespVO.KeySummary();
            ks.setTokenId(row.getLong("token_id"));
            ks.setTokenName(row.getStr("token_name"));
            ks.setRequestCount(nz(row.getLong("request_count")));
            ks.setTotalTokens(nz(row.getLong("total_tokens")));
            ks.setCostUsd(StrUtil.blankToDefault(row.getStr("cost_usd"), "0"));
            keys.add(ks);
            totalCost = totalCost.add(parseUsd(ks.getCostUsd()));
            totalTokens += ks.getTotalTokens();
            totalRequests += ks.getRequestCount();
        }
        keys.sort(Comparator.comparing(XqAiUsageDashboardRespVO.KeySummary::getTokenId,
                Comparator.nullsLast(Long::compareTo)));
        resp.setKeys(keys);

        // 明细按模型归类；Key 汇总的 categoryHint 由明细投票
        Map<Long, Map<String, Long>> keyCatVotes = new HashMap<>();
        List<XqAiUsageDashboardRespVO.UsageRow> recent = new ArrayList<>();
        for (JSONObject row : requestRows) {
            String model = StrUtil.blankToDefault(row.getStr("model_name"), "");
            String cat = classifyModel(model);
            Agg agg = byCat.get(cat);
            agg.requestCount++;
            agg.promptTokens += nz(row.getLong("prompt_tokens"));
            agg.completionTokens += nz(row.getLong("completion_tokens"));
            agg.totalTokens += nz(row.getLong("total_tokens"));
            agg.cost = agg.cost.add(parseUsd(row.getStr("cost_usd")));

            Long tid = row.getLong("token_id");
            if (tid != null) {
                keyCatVotes
                        .computeIfAbsent(tid, k -> new HashMap<>())
                        .merge(cat, 1L, Long::sum);
            }

            if (recent.size() < 100) {
                XqAiUsageDashboardRespVO.UsageRow ur = new XqAiUsageDashboardRespVO.UsageRow();
                ur.setLogId(row.getLong("log_id"));
                ur.setCreatedAt(row.getLong("created_at"));
                ur.setTokenId(tid);
                ur.setTokenName(row.getStr("token_name"));
                ur.setModelName(model);
                ur.setTaskId(row.getStr("task_id"));
                ur.setPromptTokens(nz(row.getLong("prompt_tokens")));
                ur.setCompletionTokens(nz(row.getLong("completion_tokens")));
                ur.setTotalTokens(nz(row.getLong("total_tokens")));
                ur.setCostUsd(StrUtil.blankToDefault(row.getStr("cost_usd"), "0"));
                ur.setCategory(cat);
                ur.setCategoryLabel(categoryLabel(cat));
                recent.add(ur);
            }
        }
        resp.setRecent(recent);

        for (XqAiUsageDashboardRespVO.KeySummary ks : keys) {
            Map<String, Long> votes = keyCatVotes.getOrDefault(ks.getTokenId(), Map.of());
            if (votes.isEmpty()) {
                ks.setCategoryHint("other");
            } else if (votes.size() == 1) {
                ks.setCategoryHint(votes.keySet().iterator().next());
            } else {
                ks.setCategoryHint("mixed");
            }
        }

        // 若明细为空但有 summary，把 summary 费用挂到 other，避免看板全 0
        if (requestRows.isEmpty() && !summaryRows.isEmpty()) {
            Agg other = byCat.get("other");
            other.requestCount = totalRequests;
            other.totalTokens = totalTokens;
            other.cost = totalCost;
        }

        List<XqAiUsageDashboardRespVO.CategoryBucket> cats = new ArrayList<>();
        cats.add(toBucket("copy", "文案生成", byCat.get("copy")));
        cats.add(toBucket("image_gen", "图片生成", byCat.get("image_gen")));
        cats.add(toBucket("image_revise", "修改图片", byCat.get("image_revise")));
        cats.add(toBucket("other", "其它", byCat.get("other")));
        resp.setCategories(cats);

        resp.setTotalCostUsd(fmtUsd(totalCost));
        resp.setTotalTokens(totalTokens);
        resp.setTotalRequests(totalRequests);
        return resp;
    }

    private boolean isAccountConfigured() {
        return usageProperties.isEnabled()
                && StrUtil.isNotBlank(usageProperties.getAccessToken())
                && StrUtil.isNotBlank(usageProperties.getUserId());
    }

    private void initEmptyCategories(XqAiUsageDashboardRespVO resp) {
        List<XqAiUsageDashboardRespVO.CategoryBucket> cats = new ArrayList<>();
        cats.add(emptyBucket("copy", "文案生成"));
        cats.add(emptyBucket("image_gen", "图片生成"));
        cats.add(emptyBucket("image_revise", "修改图片"));
        cats.add(emptyBucket("other", "其它"));
        resp.setCategories(cats);
    }

    private static XqAiUsageDashboardRespVO.CategoryBucket emptyBucket(String code, String label) {
        return toBucket(code, label, new Agg());
    }

    private static XqAiUsageDashboardRespVO.CategoryBucket toBucket(String code, String label, Agg a) {
        XqAiUsageDashboardRespVO.CategoryBucket b = new XqAiUsageDashboardRespVO.CategoryBucket();
        b.setCode(code);
        b.setLabel(label);
        b.setRequestCount(a.requestCount);
        b.setPromptTokens(a.promptTokens);
        b.setCompletionTokens(a.completionTokens);
        b.setTotalTokens(a.totalTokens);
        b.setCostUsd(fmtUsd(a.cost));
        return b;
    }

    private String classifyModel(String modelName) {
        String m = StrUtil.blankToDefault(modelName, "").toLowerCase(Locale.ROOT).trim();
        if (m.isEmpty()) {
            return "other";
        }
        if (matchAny(m, usageProperties.getImageReviseModelKeywords())) {
            return "image_revise";
        }
        if (matchAny(m, usageProperties.getCopyModelKeywords())) {
            // chat 模型可能含 "image" 字样时优先文案关键词已在 copy 列表；避开纯生图
            if (matchAny(m, usageProperties.getImageGenModelKeywords())
                    && !m.contains("terra")
                    && !m.contains("gpt-5")
                    && !m.contains("gpt-4")
                    && !m.contains("claude")) {
                return "image_gen";
            }
            return "copy";
        }
        if (matchAny(m, usageProperties.getImageGenModelKeywords())) {
            return "image_gen";
        }
        return "other";
    }

    private static boolean matchAny(String modelLower, List<String> keywords) {
        if (keywords == null) {
            return false;
        }
        for (String k : keywords) {
            if (StrUtil.isNotBlank(k) && modelLower.contains(k.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String categoryLabel(String code) {
        return switch (StrUtil.blankToDefault(code, "")) {
            case "copy" -> "文案生成";
            case "image_gen" -> "图片生成";
            case "image_revise" -> "修改图片";
            default -> "其它";
        };
    }

    private List<JSONObject> fetchAllPages(String path, long start, long end, Integer hardLimit) {
        List<JSONObject> out = new ArrayList<>();
        String cursor = null;
        int guard = 0;
        while (guard++ < 50) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("start_timestamp", start);
            q.put("end_timestamp", end);
            q.put("page_size", 100);
            if (StrUtil.isNotBlank(cursor)) {
                q.put("cursor", cursor);
            }
            JSONObject body = getJson(path, q);
            if (!Boolean.TRUE.equals(body.getBool("success"))) {
                String err = body.getByPath("error.message", String.class);
                throw exception(XQ_TOAPIS_USAGE_FAIL, StrUtil.blankToDefault(err, body.toString()));
            }
            JSONArray data = body.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    out.add(data.getJSONObject(i));
                    if (hardLimit != null && out.size() >= hardLimit) {
                        return out;
                    }
                }
            }
            JSONObject meta = body.getJSONObject("meta");
            boolean hasMore = meta != null && Boolean.TRUE.equals(meta.getBool("has_more"));
            cursor = meta == null ? null : meta.getStr("next_cursor");
            if (!hasMore || StrUtil.isBlank(cursor)) {
                break;
            }
        }
        return out;
    }

    private JSONObject getJson(String path, Map<String, Object> query) {
        String base = StrUtil.removeSuffix(usageProperties.getBaseUrl().trim(), "/");
        StringBuilder url = new StringBuilder(base).append(path).append('?');
        boolean first = true;
        for (Map.Entry<String, Object> e : query.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            if (!first) {
                url.append('&');
            }
            first = false;
            url.append(e.getKey()).append('=').append(e.getValue());
        }
        int timeout = Math.max(usageProperties.getConnectTimeoutMs(), usageProperties.getReadTimeoutMs());
        try (HttpResponse response = HttpRequest.get(url.toString())
                .header("Authorization", "Bearer " + usageProperties.getAccessToken().trim())
                .header("X-User-Id", usageProperties.getUserId().trim())
                .timeout(timeout)
                .execute()) {
            String raw = response.body();
            if (!response.isOk()) {
                log.warn("[toapis-usage] http={} body={}", response.getStatus(), raw);
                throw exception(XQ_TOAPIS_USAGE_FAIL, "HTTP " + response.getStatus());
            }
            return JSONUtil.parseObj(raw);
        } catch (cn.iocoder.yudao.framework.common.exception.ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[toapis-usage] fail", ex);
            throw exception(XQ_TOAPIS_USAGE_FAIL, StrUtil.blankToDefault(ex.getMessage(), "请求异常"));
        }
    }

    private List<XqAiUsageDashboardRespVO.KeyBalance> loadKeyBalances() {
        List<XqAiUsageDashboardRespVO.KeyBalance> list = new ArrayList<>();
        List<AiApiKeyDO> keys = aiApiKeyService.getApiKeyList();
        for (AiApiKeyDO key : keys) {
            if (key == null || !CommonStatusEnum.ENABLE.getStatus().equals(key.getStatus())) {
                continue;
            }
            if (!isToapisKey(key)) {
                continue;
            }
            XqAiUsageDashboardRespVO.KeyBalance b = new XqAiUsageDashboardRespVO.KeyBalance();
            b.setApiKeyDbId(key.getId());
            b.setName(key.getName());
            try {
                fillBalance(key, b);
            } catch (Exception ex) {
                b.setError(StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(), "查询失败"), 200));
            }
            list.add(b);
        }
        return list;
    }

    private static boolean isToapisKey(AiApiKeyDO key) {
        if (StrUtil.equalsIgnoreCase(key.getGatewayType(), "toapis")) {
            return true;
        }
        String url = StrUtil.blankToDefault(key.getUrl(), "").toLowerCase(Locale.ROOT);
        return url.contains("toapis");
    }

    private void fillBalance(AiApiKeyDO key, XqAiUsageDashboardRespVO.KeyBalance b) {
        String base = StrUtil.blankToDefault(key.getUrl(), usageProperties.getBaseUrl()).trim();
        base = StrUtil.removeSuffix(base, "/");
        if (base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        }
        String url = base + "/v1/balance";
        int timeout = Math.max(usageProperties.getConnectTimeoutMs(), usageProperties.getReadTimeoutMs());
        try (HttpResponse response = HttpRequest.get(url)
                .header("Authorization", "Bearer " + StrUtil.trim(key.getApiKey()))
                .timeout(timeout)
                .execute()) {
            String raw = response.body();
            JSONObject json = JSONUtil.parseObj(raw);
            if (!Boolean.TRUE.equals(json.getBool("success"))) {
                b.setError(StrUtil.blankToDefault(json.getStr("message"), "余额查询失败"));
                return;
            }
            b.setUnlimitedQuota(Boolean.TRUE.equals(json.getBool("unlimited_quota")));
            b.setRemainBalance(String.valueOf(json.get("remain_balance")));
            b.setUsedBalance(String.valueOf(json.get("used_balance")));
            b.setRemainCredits(String.valueOf(json.get("remain_credits")));
            b.setUsedCredits(String.valueOf(json.get("used_credits")));
        }
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal parseUsd(String s) {
        try {
            return new BigDecimal(StrUtil.blankToDefault(s, "0").trim());
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private static String fmtUsd(BigDecimal v) {
        return v.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static final class Agg {
        long requestCount;
        long promptTokens;
        long completionTokens;
        long totalTokens;
        BigDecimal cost = BigDecimal.ZERO;
    }
}
