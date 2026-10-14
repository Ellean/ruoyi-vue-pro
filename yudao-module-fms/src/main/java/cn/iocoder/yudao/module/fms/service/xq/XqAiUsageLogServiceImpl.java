package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage.XqWorkOrderAiUsageVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqAiUsageLogDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqAiUsageLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class XqAiUsageLogServiceImpl implements XqAiUsageLogService {

    /**
     * ToAPIs 公开单价估算（非账单）：
     * - 生图/改图：GPT Image 1K ≈ $0.015 / 张
     * - 文案：gpt-5.6-terra 积分折算 ≈ 输入 $0.40 / 1M、输出 $2.40 / 1M
     *   （80 credit/1M in、480 credit/1M out，$0.015≈3 credit → $0.005/credit）
     */
    private static final BigDecimal IMAGE_USD_PER_CALL = new BigDecimal("0.015");
    private static final BigDecimal COPY_USD_PER_1M_PROMPT = new BigDecimal("0.40");
    private static final BigDecimal COPY_USD_PER_1M_COMPLETION = new BigDecimal("2.40");
    /** 无 token 明细时，单次文案兜底 */
    private static final BigDecimal COPY_USD_PER_CALL_FALLBACK = new BigDecimal("0.01");
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    @Resource
    private XqAiUsageLogMapper usageLogMapper;

    @Override
    public void record(Long workOrderId, String category, String model,
                       Integer promptTokens, Integer completionTokens,
                       Integer callCount, BigDecimal costUsd, String remark) {
        if (workOrderId == null || StrUtil.isBlank(category)) {
            return;
        }
        int pt = promptTokens == null ? 0 : Math.max(0, promptTokens);
        int ct = completionTokens == null ? 0 : Math.max(0, completionTokens);
        int calls = callCount == null || callCount < 1 ? 1 : callCount;
        int total = pt + ct;
        BigDecimal cost = costUsd;
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            cost = estimateCost(category.trim(), calls, pt, ct);
        }
        try {
            XqAiUsageLogDO row = XqAiUsageLogDO.builder()
                    .workOrderId(workOrderId)
                    .category(category.trim())
                    .model(StrUtil.maxLength(StrUtil.blankToDefault(model, ""), 120))
                    .promptTokens(pt)
                    .completionTokens(ct)
                    .totalTokens(total)
                    .callCount(calls)
                    .costUsd(cost)
                    .remark(StrUtil.maxLength(StrUtil.blankToDefault(remark, ""), 250))
                    .build();
            usageLogMapper.insert(row);
        } catch (Exception ex) {
            // 记账失败不影响主流程
            log.warn("[ai-usage-log] record fail wo={} cat={} err={}",
                    workOrderId, category, ex.getMessage());
        }
    }

    @Override
    public Map<Long, XqWorkOrderAiUsageVO> summarizeByWorkOrderIds(Collection<Long> workOrderIds) {
        if (CollUtil.isEmpty(workOrderIds)) {
            return Collections.emptyMap();
        }
        List<Long> ids = new ArrayList<>();
        for (Long id : workOrderIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, XqWorkOrderAiUsageVO> map = new HashMap<>();
        for (Long id : ids) {
            map.put(id, emptyVo(id));
        }
        List<XqAiUsageLogDO> rows;
        try {
            rows = usageLogMapper.selectByWorkOrderIds(ids);
        } catch (Exception ex) {
            // 未建表时仍返回 0 占用展示，避免整页挂掉
            log.warn("[ai-usage-log] summarize skip: {}", ex.getMessage());
            return map;
        }
        for (XqAiUsageLogDO row : rows) {
            XqWorkOrderAiUsageVO vo = map.computeIfAbsent(row.getWorkOrderId(),
                    XqAiUsageLogServiceImpl::emptyVo);
            int calls = row.getCallCount() == null ? 1 : row.getCallCount();
            long tokens = row.getTotalTokens() == null ? 0L : row.getTotalTokens();
            vo.setTotalCalls(vo.getTotalCalls() + calls);
            vo.setTotalTokens(vo.getTotalTokens() + tokens);
            String cat = StrUtil.blankToDefault(row.getCategory(), "");
            switch (cat) {
                case CAT_COPY -> {
                    vo.setCopyCalls(vo.getCopyCalls() + calls);
                    vo.setCopyTokens(vo.getCopyTokens() + tokens);
                }
                case CAT_IMAGE_GEN -> {
                    vo.setImageGenCalls(vo.getImageGenCalls() + calls);
                    vo.setImageGenTokens(vo.getImageGenTokens() + tokens);
                }
                case CAT_IMAGE_REVISE -> {
                    vo.setImageReviseCalls(vo.getImageReviseCalls() + calls);
                    vo.setImageReviseTokens(vo.getImageReviseTokens() + tokens);
                }
                default -> {
                }
            }
            BigDecimal rowCost = row.getCostUsd();
            if (rowCost == null || rowCost.compareTo(BigDecimal.ZERO) <= 0) {
                int pt = row.getPromptTokens() == null ? 0 : row.getPromptTokens();
                int ct = row.getCompletionTokens() == null ? 0 : row.getCompletionTokens();
                rowCost = estimateCost(cat, calls, pt, ct);
            }
            vo.setEstimatedCostUsd(nz(vo.getEstimatedCostUsd()).add(rowCost));
        }
        for (XqWorkOrderAiUsageVO vo : map.values()) {
            finishVo(vo);
        }
        return map;
    }

    private static XqWorkOrderAiUsageVO emptyVo(Long id) {
        XqWorkOrderAiUsageVO vo = new XqWorkOrderAiUsageVO();
        vo.setWorkOrderId(id);
        vo.setCopyCalls(0);
        vo.setCopyTokens(0L);
        vo.setImageGenCalls(0);
        vo.setImageGenTokens(0L);
        vo.setImageReviseCalls(0);
        vo.setImageReviseTokens(0L);
        vo.setTotalCalls(0);
        vo.setTotalTokens(0L);
        vo.setEstimatedCostUsd(BigDecimal.ZERO);
        finishVo(vo);
        return vo;
    }

    private static void finishVo(XqWorkOrderAiUsageVO vo) {
        BigDecimal cost = nz(vo.getEstimatedCostUsd()).setScale(4, RoundingMode.HALF_UP);
        vo.setEstimatedCostUsd(cost);
        vo.setCostLabel("约$" + trimUsd(cost));
        vo.setLabel(buildLabel(vo));
    }

    /** 始终展示次数 + 预计费用 */
    private static String buildLabel(XqWorkOrderAiUsageVO vo) {
        int copy = vo.getCopyCalls() == null ? 0 : vo.getCopyCalls();
        int gen = vo.getImageGenCalls() == null ? 0 : vo.getImageGenCalls();
        int revise = vo.getImageReviseCalls() == null ? 0 : vo.getImageReviseCalls();
        String cost = StrUtil.blankToDefault(vo.getCostLabel(), "约$0");
        return "文案" + copy + " · 生图" + gen + " · 改图" + revise + " · " + cost;
    }

    private static BigDecimal estimateCost(String category, int calls, int promptTokens, int completionTokens) {
        int c = Math.max(1, calls);
        if (CAT_COPY.equals(category)) {
            if (promptTokens > 0 || completionTokens > 0) {
                BigDecimal in = BigDecimal.valueOf(promptTokens)
                        .multiply(COPY_USD_PER_1M_PROMPT)
                        .divide(MILLION, 8, RoundingMode.HALF_UP);
                BigDecimal out = BigDecimal.valueOf(completionTokens)
                        .multiply(COPY_USD_PER_1M_COMPLETION)
                        .divide(MILLION, 8, RoundingMode.HALF_UP);
                return in.add(out);
            }
            return COPY_USD_PER_CALL_FALLBACK.multiply(BigDecimal.valueOf(c));
        }
        // 生图 / 改图 / 其它：按次
        return IMAGE_USD_PER_CALL.multiply(BigDecimal.valueOf(c));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String trimUsd(BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return cost.stripTrailingZeros().toPlainString();
    }
}
