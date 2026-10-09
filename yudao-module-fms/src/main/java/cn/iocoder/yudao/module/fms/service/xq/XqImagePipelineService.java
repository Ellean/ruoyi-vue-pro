package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaImageCallbackReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;

import java.util.List;
import java.util.Map;

/**
 * 生图流水线：主 API 只入队/组包/落库；重活由生图 RPA worker 执行。
 */
public interface XqImagePipelineService {

    /** 组装单条生图任务给 RPA 的 inputParam */
    Map<String, Object> buildJobInput(XqWorkOrderDO order, Long userId);

    /** 写入美工待跑生图队列，不触发 Commander */
    void enqueueImageJob(XqWorkOrderDO order, Long userId);

    /** 批量入队；返回成功条数 */
    int enqueueImageJobs(List<Long> ids, Long userId);

    /** RPA：按当前登录美工领取待跑生图 */
    List<Map<String, Object>> pullImageJobs(Long userId, Integer limit);

    /** 按任务 ID 返回生图任务包 */
    Map<String, Object> buildImageDetailById(Long userId, Long workOrderId);

    /** 生图 RPA 回调写回成品图 */
    void handleCallback(XqWorkOrderRpaImageCallbackReqVO reqVO);

}
