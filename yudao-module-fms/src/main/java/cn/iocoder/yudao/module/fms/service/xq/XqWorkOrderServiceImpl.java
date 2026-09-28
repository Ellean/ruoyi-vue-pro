package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderAssignImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderBatchIdsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderDispatchReqVO;
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

import java.util.ArrayList;
import java.util.List;

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
    public List<XqWorkOrderDO> dispatchFromLibrary(XqWorkOrderDispatchReqVO reqVO, Long userId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        if (StrUtil.hasBlank(reqVO.getListingPlatformId(), reqVO.getListingCategoryId())) {
            throw exception(XQ_DISPATCH_LISTING_REQUIRED);
        }
        List<XqWorkOrderDO> created = new ArrayList<>();
        for (XqWorkOrderDispatchReqVO.Item item : reqVO.getItems()) {
            String sku = StrUtil.trim(item.getSku());
            if (StrUtil.isBlank(sku)) {
                continue;
            }
            XqWorkOrderDO exists = workOrderMapper.selectDoingByExternalSku(sku);
            if (exists != null) {
                // 已在工作台进行中：跳过，不重复建单
                created.add(exists);
                continue;
            }
            String title = StrUtil.blankToDefault(item.getTitle(), sku);
            XqWorkOrderDO order = XqWorkOrderDO.builder()
                    .no("WO" + IdUtil.getSnowflakeNextIdStr())
                    .sourceId(null)
                    .externalSku(sku)
                    .title(title)
                    .coverUrl(item.getCoverUrl())
                    .categoryName(item.getCategoryName())
                    .gigaCategoryId(item.getGigaCategoryId())
                    .status(WORK_STATUS_DOING)
                    .assigneeUserId(userId)
                    .listingPlatformId(reqVO.getListingPlatformId())
                    .listingShopId(reqVO.getListingShopId())
                    .listingCategoryId(reqVO.getListingCategoryId())
                    .listingPlatformName(reqVO.getListingPlatformName())
                    .listingShopName(reqVO.getListingShopName())
                    .listingCategoryName(reqVO.getListingCategoryName())
                    .workflowPhase("copy")
                    .build();
            workOrderMapper.insert(order);
            created.add(order);
        }
        if (created.isEmpty()) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        return created;
    }

    @Override
    public XqWorkOrderDO generateCopy(Long id, Long userId) {
        return generateCopyInternal(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<XqWorkOrderDO> batchGenerateCopy(XqWorkOrderBatchIdsReqVO reqVO, Long userId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        List<XqWorkOrderDO> result = new ArrayList<>();
        for (Long id : reqVO.getIds()) {
            result.add(generateCopyInternal(id, userId));
        }
        return result;
    }

    private XqWorkOrderDO generateCopyInternal(Long id, Long claimUserId) {
        XqWorkOrderDO order = validateDoing(id);
        String sku = order.getExternalSku();
        String title = StrUtil.blankToDefault(order.getTitle(), sku);
        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setContentTitle(title + " | Premium Listing");
        update.setContentSellingPoints(
                "• Durable build for daily use\n"
                        + "• Clean modern design, ready to list\n"
                        + "• Item Code: " + sku + "\n"
                        + "• Generated for marketplace copy (demo)");
        update.setWorkflowPhase("image");
        if (claimUserId != null) {
            update.setCopyUserId(claimUserId);
            if (order.getAssigneeUserId() == null) {
                update.setAssigneeUserId(claimUserId);
            }
        }
        workOrderMapper.updateById(update);
        return workOrderMapper.selectById(id);
    }

    @Override
    public XqWorkOrderDO generateImage(Long id) {
        XqWorkOrderDO order = validateDoing(id);
        // 原逻辑：必须先文案，再图片
        if (!isCopyReady(order)) {
            throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
        }
        String image = StrUtil.blankToDefault(order.getCoverUrl(), "");
        if (StrUtil.isBlank(image)) {
            image = "https://via.placeholder.com/800x800.png?text=" + order.getExternalSku();
        }
        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setGeneratedImageUrl(image);
        update.setWorkflowPhase("list");
        workOrderMapper.updateById(update);
        return workOrderMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAssignImage(XqWorkOrderAssignImageReqVO reqVO, Long operatorUserId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        int count = 0;
        for (Long id : reqVO.getIds()) {
            XqWorkOrderDO order = validateDoing(id);
            if (!isCopyReady(order)) {
                throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
            }
            // 仅本人领取的文案可分配（超管/本人 copyUser 为空时允许操作人）
            if (order.getCopyUserId() != null
                    && operatorUserId != null
                    && !order.getCopyUserId().equals(operatorUserId)) {
                continue;
            }
            XqWorkOrderDO update = new XqWorkOrderDO();
            update.setId(id);
            update.setImageUserId(reqVO.getImageUserId());
            update.setWorkflowPhase("image");
            if (StrUtil.isNotBlank(reqVO.getListingPlatformId())) {
                update.setListingPlatformId(reqVO.getListingPlatformId());
            }
            if (StrUtil.isNotBlank(reqVO.getListingShopId())) {
                update.setListingShopId(reqVO.getListingShopId());
            }
            if (order.getCopyUserId() == null && operatorUserId != null) {
                update.setCopyUserId(operatorUserId);
            }
            workOrderMapper.updateById(update);
            count++;
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long completeWorkOrder(XqWorkOrderCompleteReqVO completeReqVO) {
        XqWorkOrderDO order = validateDoing(completeReqVO.getId());
        if (!isCopyReady(order)) {
            throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
        }
        if (!isImageReady(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        // SKU 唯一
        if (productMapper.selectBySku(completeReqVO.getProductSku()) != null) {
            throw exception(XQ_PRODUCT_SKU_DUPLICATE);
        }
        XqProductDO product = XqProductDO.builder()
                .sku(completeReqVO.getProductSku())
                .name(completeReqVO.getProductName())
                .categoryName(completeReqVO.getCategoryName())
                .imageUrl(StrUtil.blankToDefault(order.getGeneratedImageUrl(), order.getCoverUrl()))
                .status(CommonStatusEnum.ENABLE.getStatus())
                .remark("工作台上架:" + order.getNo())
                .build();
        productMapper.insert(product);

        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setStatus(WORK_STATUS_DONE);
        update.setProductId(product.getId());
        update.setProductSku(product.getSku());
        update.setWorkflowPhase("done");
        if (completeReqVO.getProductName() != null) {
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

    private static boolean isCopyReady(XqWorkOrderDO order) {
        return StrUtil.isNotBlank(order.getContentTitle());
    }

    private static boolean isImageReady(XqWorkOrderDO order) {
        return StrUtil.isNotBlank(order.getGeneratedImageUrl());
    }

}
