package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqProductDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.service.xq.XqSourceItemServiceImpl.WORK_STATUS_DOING;

@Service
@Validated
public class XqWorkOrderServiceImpl implements XqWorkOrderService {

    public static final int WORK_STATUS_DONE = 20;

    @Resource
    private XqWorkOrderMapper workOrderMapper;
    @Resource
    private XqProductMapper productMapper;

    @Override
    public PageResult<XqWorkOrderDO> getWorkOrderPage(XqWorkOrderPageReqVO pageReqVO) {
        return workOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public XqWorkOrderDO getWorkOrder(Long id) {
        return workOrderMapper.selectById(id);
    }

    @Override
    public void updateWorkOrder(XqWorkOrderUpdateReqVO updateReqVO) {
        XqWorkOrderDO order = validateDoing(updateReqVO.getId());
        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setContentTitle(updateReqVO.getContentTitle());
        update.setContentSellingPoints(updateReqVO.getContentSellingPoints());
        workOrderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long completeWorkOrder(XqWorkOrderCompleteReqVO completeReqVO) {
        XqWorkOrderDO order = validateDoing(completeReqVO.getId());
        // SKU 唯一
        if (productMapper.selectBySku(completeReqVO.getProductSku()) != null) {
            throw exception(XQ_PRODUCT_SKU_DUPLICATE);
        }
        XqProductDO product = XqProductDO.builder()
                .sku(completeReqVO.getProductSku())
                .name(completeReqVO.getProductName())
                .categoryName(completeReqVO.getCategoryName())
                .status(CommonStatusEnum.ENABLE.getStatus())
                .remark("作业入库:" + order.getNo())
                .build();
        productMapper.insert(product);

        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setStatus(WORK_STATUS_DONE);
        update.setProductId(product.getId());
        update.setProductSku(product.getSku());
        if (completeReqVO.getProductName() != null) {
            // 若未填文案标题，用产品名兜底
            if (order.getContentTitle() == null || order.getContentTitle().isEmpty()) {
                update.setContentTitle(completeReqVO.getProductName());
            }
        }
        workOrderMapper.updateById(update);
        return product.getId();
    }

    private XqWorkOrderDO validateDoing(Long id) {
        XqWorkOrderDO order = workOrderMapper.selectById(id);
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        return order;
    }

}
