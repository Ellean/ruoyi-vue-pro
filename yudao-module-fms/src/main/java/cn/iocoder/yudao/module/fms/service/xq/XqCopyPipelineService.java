package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaCopyCallbackReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;

import java.util.List;
import java.util.Map;

/**
 * 文案流水线：规则约束 → 文案（卖点/突出内容）→ 识图分型 → 图片提示词
 * 重活由文案 RPA 执行；本服务负责组包触发与回调落库。
 */
public interface XqCopyPipelineService {

    /** 组装单条任务给 RPA 的 inputParam（含规则、原图、原文案、回调） */
    Map<String, Object> buildJobInput(XqWorkOrderDO order, Long userId);

    /** 触发文案 RPA；返回 workUuid */
    String triggerCopyJob(XqWorkOrderDO order, Long userId);

    /** 批量触发（逐条入队） */
    List<XqWorkOrderDO> triggerBatch(List<Long> ids, Long userId);

    /** RPA 回调写回文案与图片提示词 */
    void handleCallback(XqWorkOrderRpaCopyCallbackReqVO reqVO);

    /** 按登录人领取待跑文案（精简 SKU 列表） */
    List<Map<String, Object>> pullCopyJobs(Long userId, Integer limit);

    /** 按 SKU 补齐原文案/原图/规则后返回 RPA 任务包 */
    Map<String, Object> buildCopyDetailBySku(Long userId, String sku);

}
