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
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqProductDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.service.xq.XqSourceItemServiceImpl.WORK_STATUS_DOING;

@Service
@Validated
public class XqWorkOrderServiceImpl implements XqWorkOrderService {

    public static final int WORK_STATUS_DONE = 20;
    public static final int WORK_STATUS_CLOSED = 30;

    @Resource
    private XqWorkOrderMapper workOrderMapper;
    @Resource
    private XqProductMapper productMapper;
    @Resource
    private XqCopyPipelineService copyPipelineService;
    @Resource
    private XqGigaProductMapper gigaProductMapper;

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
        if (StrUtil.isNotBlank(updateReqVO.getCopyResultJson())) {
            update.setCopyResultJson(updateReqVO.getCopyResultJson());
        } else if (updateReqVO.getContentHighlight() != null
                || updateReqVO.getContentTitle() != null
                || updateReqVO.getContentSellingPoints() != null) {
            // 合并突出内容到 copy_result_json，供右侧编辑区回显
            cn.hutool.json.JSONObject json = StrUtil.isNotBlank(order.getCopyResultJson())
                    ? JSONUtil.parseObj(order.getCopyResultJson())
                    : JSONUtil.createObj();
            if (updateReqVO.getContentTitle() != null) {
                json.set("title", updateReqVO.getContentTitle());
            }
            if (updateReqVO.getContentSellingPoints() != null) {
                json.set("sellingPoints", updateReqVO.getContentSellingPoints());
            }
            if (updateReqVO.getContentHighlight() != null) {
                json.set("highlightStyle", updateReqVO.getContentHighlight());
                json.set("description", updateReqVO.getContentHighlight());
            }
            update.setCopyResultJson(json.toString());
        }
        if (updateReqVO.getImagePromptJson() != null) {
            update.setImagePromptJson(updateReqVO.getImagePromptJson());
        }
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
        // 先校验：同 SKU + 同平台 已有进行中/已上架 → 硬拦截（不同平台可并行）
        for (XqWorkOrderDispatchReqVO.Item item : reqVO.getItems()) {
            String sku = StrUtil.trim(item.getSku());
            if (StrUtil.isBlank(sku)) {
                continue;
            }
            XqWorkOrderDO exists = workOrderMapper.selectActiveBySkuAndPlatform(
                    sku, reqVO.getListingPlatformId());
            if (exists != null) {
                throw exception(XQ_WORK_ORDER_ALREADY_EXISTS, sku);
            }
        }

        List<XqWorkOrderDO> created = new ArrayList<>();
        for (XqWorkOrderDispatchReqVO.Item item : reqVO.getItems()) {
            String sku = StrUtil.trim(item.getSku());
            if (StrUtil.isBlank(sku)) {
                continue;
            }
            String title = StrUtil.blankToDefault(item.getTitle(), sku);
            String coverUrl = item.getCoverUrl();
            String sourceDescription = null;
            String sourceImageUrls = null;
            String gigaProductId = StrUtil.trim(item.getProductId());
            XqGigaProductRow giga = null;
            if (StrUtil.isNotBlank(gigaProductId)) {
                giga = gigaProductMapper.selectById(gigaProductId);
            }
            if (giga == null) {
                giga = gigaProductMapper.selectBySku(sku);
            }
            if (giga != null) {
                gigaProductId = giga.getId();
                sourceDescription = giga.getDescription();
                List<String> urls = extractImageUrls(giga.getImageUrlsJson(), giga.getImageUrl());
                if (!urls.isEmpty()) {
                    sourceImageUrls = JSONUtil.toJsonStr(urls);
                    if (StrUtil.isBlank(coverUrl)) {
                        coverUrl = urls.get(0);
                    }
                }
                if (StrUtil.isBlank(title)) {
                    title = StrUtil.blankToDefault(giga.getName(), sku);
                }
            }
            XqWorkOrderDO order = XqWorkOrderDO.builder()
                    .no("WO" + IdUtil.getSnowflakeNextIdStr())
                    .sourceId(null)
                    .gigaProductId(gigaProductId)
                    .externalSku(sku)
                    .title(title)
                    .coverUrl(coverUrl)
                    .sourceDescription(sourceDescription)
                    .sourceImageUrls(sourceImageUrls)
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
                    .rpaCopyStatus("idle")
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
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id) {
        XqWorkOrderDO order = workOrderMapper.selectById(id);
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_CLOSE_INVALID);
        }
        // 关闭 + 清空文案/美工/生成图等进度，避免残留在「我的文案」
        workOrderMapper.closeAndClearProgress(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCloseWorkOrder(XqWorkOrderBatchIdsReqVO reqVO) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        int count = 0;
        for (Long id : reqVO.getIds()) {
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null) {
                continue;
            }
            if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
                continue;
            }
            workOrderMapper.closeAndClearProgress(id);
            count++;
        }
        return count;
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
        // 主 API 只触发文案 RPA（规则→文案→识图→图提示词）；结果走回调落库
        copyPipelineService.triggerCopyJob(order, claimUserId);
        return workOrderMapper.selectById(id);
    }

    private static List<String> extractImageUrls(String imageUrlsJson, String cover) {
        Set<String> set = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(cover)) {
            set.add(cover.trim());
        }
        if (StrUtil.isNotBlank(imageUrlsJson) && !"null".equalsIgnoreCase(imageUrlsJson)) {
            try {
                for (Object o : JSONUtil.parseArray(imageUrlsJson)) {
                    String url = StrUtil.trim(String.valueOf(o));
                    if (StrUtil.isNotBlank(url) && !"null".equalsIgnoreCase(url)) {
                        set.add(url);
                    }
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return new ArrayList<>(set);
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
