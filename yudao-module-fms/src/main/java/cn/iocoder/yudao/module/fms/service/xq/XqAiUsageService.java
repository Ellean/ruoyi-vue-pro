package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage.XqAiUsageDashboardRespVO;

/**
 * XQ 产品侧 AI 用量计费（对接 ToAPIs）。
 */
public interface XqAiUsageService {

    /**
     * @param startTimestamp Unix 秒，可空则默认近 7 天
     * @param endTimestamp   Unix 秒，可空则默认现在
     */
    XqAiUsageDashboardRespVO getDashboard(Long startTimestamp, Long endTimestamp);
}
