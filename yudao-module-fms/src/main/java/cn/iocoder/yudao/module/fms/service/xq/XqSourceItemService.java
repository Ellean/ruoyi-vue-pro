package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source.XqSourceItemPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSourceItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;

public interface XqSourceItemService {

    PageResult<XqSourceItemDO> getSourcePage(XqSourceItemPageReqVO pageReqVO);

    XqSourceItemDO getSource(Long id);

    /** 认领货源并生成作业单，返回作业单 */
    XqWorkOrderDO claimSource(Long sourceId, Long userId);

}
