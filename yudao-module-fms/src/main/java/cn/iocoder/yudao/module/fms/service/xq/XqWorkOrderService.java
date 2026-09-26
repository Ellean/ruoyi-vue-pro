package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;

import jakarta.validation.Valid;

public interface XqWorkOrderService {

    PageResult<XqWorkOrderDO> getWorkOrderPage(XqWorkOrderPageReqVO pageReqVO);

    XqWorkOrderDO getWorkOrder(Long id);

    void updateWorkOrder(@Valid XqWorkOrderUpdateReqVO updateReqVO);

    /** 完成入库：写品库 + 关作业 */
    Long completeWorkOrder(@Valid XqWorkOrderCompleteReqVO completeReqVO);

}
