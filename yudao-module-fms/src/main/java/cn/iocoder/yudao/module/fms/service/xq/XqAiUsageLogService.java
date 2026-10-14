package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage.XqWorkOrderAiUsageVO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface XqAiUsageLogService {

    String CAT_COPY = "copy";
    String CAT_IMAGE_GEN = "image_gen";
    String CAT_IMAGE_REVISE = "image_revise";

    void record(Long workOrderId, String category, String model,
                Integer promptTokens, Integer completionTokens,
                Integer callCount, BigDecimal costUsd, String remark);

    /** 按工单汇总消耗 */
    Map<Long, XqWorkOrderAiUsageVO> summarizeByWorkOrderIds(Collection<Long> workOrderIds);
}
