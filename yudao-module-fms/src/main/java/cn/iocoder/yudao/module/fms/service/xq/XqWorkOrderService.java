package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqAssignableImageUserRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderAssignImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderBatchIdsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderDispatchReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderImageStatusReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderListReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCopyRpaEnqueueReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRegenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;

import jakarta.validation.Valid;

import java.util.List;

public interface XqWorkOrderService {

    PageResult<XqWorkOrderDO> getWorkOrderPage(XqWorkOrderPageReqVO pageReqVO);

    XqWorkOrderDO getWorkOrder(Long id);

    void updateWorkOrder(@Valid XqWorkOrderUpdateReqVO updateReqVO);

    List<XqWorkOrderDO> dispatchFromLibrary(@Valid XqWorkOrderDispatchReqVO reqVO, Long userId);

    void attachVariants(List<cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO> list);

    XqWorkOrderDO generateCopy(Long id, Long userId);

    /** AI 重新生成：参考现有文案 + 用户提示词二次修改 */
    XqWorkOrderDO regenerateCopy(@Valid XqWorkOrderRegenerateCopyReqVO reqVO, Long userId);

    /**
     * 按勾选入队文案 RPA：主体必跑；勾选变体各自入队；未勾选变体待主体成功后沿用主体文案。
     * @return 实际入队条数
     */
    int enqueueCopyRpa(@Valid XqWorkOrderCopyRpaEnqueueReqVO reqVO, Long userId);

    /** 批量生成文案：领取为当前用户文案任务 */
    List<XqWorkOrderDO> batchGenerateCopy(@Valid XqWorkOrderBatchIdsReqVO reqVO, Long userId);

    XqWorkOrderDO generateImage(Long id);

    /** 可分配美工：xq_image 角色用户 + 当前操作人 */
    List<XqAssignableImageUserRespVO> listAssignableImageUsers(Long operatorUserId);

    /** 批量把已完成文案的任务分配给美工 */
    int batchAssignImage(@Valid XqWorkOrderAssignImageReqVO reqVO, Long operatorUserId);

    void updateImageStatus(@Valid XqWorkOrderImageStatusReqVO reqVO);

    Long listWorkOrder(@Valid XqWorkOrderListReqVO reqVO);

    Long completeWorkOrder(@Valid XqWorkOrderCompleteReqVO completeReqVO);

    /** 关闭进行中任务并清理文案/美工进度（关闭后可同平台再下发） */
    void closeWorkOrder(Long id);

    /** 批量关闭并清理进度 */
    int batchCloseWorkOrder(@Valid XqWorkOrderBatchIdsReqVO reqVO);

}
