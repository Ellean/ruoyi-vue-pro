package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqAssignableImageUserRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderAssignImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderBindListingReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderBatchIdsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderDispatchReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderImageStatusReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderListReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCopyRpaEnqueueReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRegenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReuseCandidateRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReuseLookupReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReviseImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReviseImageRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateImageReqVO;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqProductDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqListingPlatformCredentialDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import cn.iocoder.yudao.module.fms.service.xq.listing.XqListingExportResult;
import cn.iocoder.yudao.module.fms.service.xq.listing.XqListingSplitExportService;
import cn.iocoder.yudao.module.fms.service.xq.listing.XqMiraklListingClient;
import cn.iocoder.yudao.module.fms.service.xq.listing.XqWayfairListingClient;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.service.xq.XqSourceItemServiceImpl.WORK_STATUS_DOING;

@Service
@Validated
@Slf4j
public class XqWorkOrderServiceImpl implements XqWorkOrderService {

    public static final int WORK_STATUS_DONE = 20;
    public static final int WORK_STATUS_CLOSED = 30;
    /** 美工概念锚点：菜单权限，各租户用自己的角色去授权，不写死角色 id */
    public static final String PERM_XQ_IMAGE = "xq:work-order:my-image";

    @Resource
    private XqWorkOrderMapper workOrderMapper;
    @Resource
    private XqProductMapper productMapper;
    @Resource
    private XqCopyPipelineService copyPipelineService;
    @Resource
    private XqImagePipelineService imagePipelineService;
    @Resource
    private XqRpaAiService rpaAiService;
    @Resource
    private XqGigaProductMapper gigaProductMapper;
    @Resource
    private XqStoreScopeService storeScopeService;
    @Resource
    private XqGigaCategoryService gigaCategoryService;
    @Resource
    private XqListingModeHelper listingModeHelper;
    @Resource
    private XqListingCredentialService listingCredentialService;
    @Resource
    private XqListingSplitExportService listingSplitExportService;
    @Resource
    private XqMiraklListingClient miraklListingClient;
    @Resource
    private XqWayfairListingClient wayfairListingClient;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public PageResult<XqWorkOrderDO> getWorkOrderPage(XqWorkOrderPageReqVO pageReqVO) {
        if (pageReqVO != null && pageReqVO.getGigaCategoryId() != null
                && (pageReqVO.getGigaCategoryIds() == null || pageReqVO.getGigaCategoryIds().isEmpty())) {
            // 大健云仓类目：选父级时含子树
            pageReqVO.setGigaCategoryIds(
                    gigaCategoryService.listSubtreeGigaIds(pageReqVO.getGigaCategoryId()));
            pageReqVO.setGigaCategoryId(null);
        }
        PageResult<XqWorkOrderDO> page = workOrderMapper.selectPage(pageReqVO);
        // 纠正历史「合并父/家族变体原图」污染，图片池刷新即可看到本 SKU 图
        if (page != null && CollUtil.isNotEmpty(page.getList())) {
            for (XqWorkOrderDO row : page.getList()) {
                try {
                    copyPipelineService.enrichSourceIfNeeded(row);
                } catch (Exception ignored) {
                    // 补齐失败不阻断列表
                }
            }
        }
        return page;
    }

    @Override
    public XqWorkOrderDO getWorkOrder(Long id) {
        XqWorkOrderDO order = workOrderMapper.selectById(id);
        if (order == null) {
            return null;
        }
        assertOwner(order);
        try {
            copyPipelineService.enrichSourceIfNeeded(order);
        } catch (Exception ignored) {
            // ignore
        }
        return order;
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
                || updateReqVO.getContentDescription() != null
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
                String raw = updateReqVO.getContentHighlight().trim();
                if (raw.startsWith("[")) {
                    try {
                        json.set("highlightStyle", JSONUtil.parseArray(raw));
                    } catch (Exception ex) {
                        json.set("highlightStyle", raw);
                    }
                } else if (raw.contains("、") || raw.contains(",") || raw.contains("，")) {
                    json.set("highlightStyle", JSONUtil.parseArray(
                            JSONUtil.toJsonStr(java.util.Arrays.stream(raw.split("[,，、]+"))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .toList())));
                } else {
                    json.set("highlightStyle", raw);
                }
            }
            if (updateReqVO.getContentDescription() != null) {
                json.set("description", updateReqVO.getContentDescription());
            }
            update.setCopyResultJson(json.toString());
        }
        if (updateReqVO.getImagePromptJson() != null) {
            update.setImagePromptJson(updateReqVO.getImagePromptJson());
        }
        if (updateReqVO.getGeneratedImageUrl() != null) {
            update.setGeneratedImageUrl(updateReqVO.getGeneratedImageUrl());
        }
        if (updateReqVO.getListingValuesJson() != null) {
            update.setListingValuesJson(updateReqVO.getListingValuesJson());
        }
        if (updateReqVO.getImagePromptJson() != null || updateReqVO.getGeneratedImageUrl() != null) {
            boolean hasAi = StrUtil.isNotBlank(updateReqVO.getGeneratedImageUrl())
                    || StrUtil.isNotBlank(order.getGeneratedImageUrl());
            if ("rejected".equals(order.getImageStatus()) && hasAi) {
                update.setImageStatus("revised");
            } else if (hasAi && ("pending".equals(order.getImageStatus())
                    || "todo".equals(order.getImageStatus())
                    || StrUtil.isBlank(order.getImageStatus()))) {
                update.setImageStatus("generated");
            } else if (order.getImageUserId() != null && StrUtil.isBlank(order.getImageStatus())) {
                update.setImageStatus("pending");
            }
        }
        if (updateReqVO.getListingCategoryId() != null) {
            update.setListingCategoryId(updateReqVO.getListingCategoryId());
        }
        if (updateReqVO.getListingCategoryName() != null) {
            update.setListingCategoryName(updateReqVO.getListingCategoryName());
        }
        if (updateReqVO.getListingShopId() != null) {
            update.setListingShopId(updateReqVO.getListingShopId());
        }
        if (updateReqVO.getListingShopName() != null) {
            update.setListingShopName(updateReqVO.getListingShopName());
        }
        if (updateReqVO.getListingCountryCode() != null) {
            update.setListingCountryCode(updateReqVO.getListingCountryCode());
        }
        if (updateReqVO.getListingCountryName() != null) {
            update.setListingCountryName(updateReqVO.getListingCountryName());
        }
        workOrderMapper.updateById(update);
    }

    @Override
    public List<XqWorkOrderDO> dispatchFromLibrary(XqWorkOrderDispatchReqVO reqVO, Long userId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        if (StrUtil.isBlank(reqVO.getListingPlatformId())) {
            throw exception(XQ_DISPATCH_LISTING_REQUIRED);
        }
        // 平台/店铺以主库 xq_* 为准；店铺范围挂在芋道用户上（xq_user_store）
        if (storeScopeService.getPlatform(XqStoreScopeService.parseLong(reqVO.getListingPlatformId())) == null) {
            throw exception(XQ_PLATFORM_NOT_EXISTS);
        }
        if (StrUtil.isNotBlank(reqVO.getListingShopId())) {
            Long shopId = XqStoreScopeService.parseLong(reqVO.getListingShopId());
            if (storeScopeService.getStore(shopId) == null) {
                throw exception(XQ_STORE_NOT_EXISTS);
            }
            if (!storeScopeService.isStoreAllowed(userId, shopId)) {
                throw exception(XQ_STORE_ACCESS_DENIED);
            }
        } else if (storeScopeService.getAllowedStoreIds(userId) != null) {
            throw exception(XQ_STORE_ACCESS_DENIED);
        }
        List<ResolvedItem> resolved = expandDispatchItems(reqVO.getItems());
        // 先校验：同 SKU + 同平台 已有进行中/已上架 → 硬拦截（不同平台可并行）
        for (ResolvedItem item : resolved) {
            if (Boolean.TRUE.equals(item.selected) && StrUtil.isNotBlank(item.sku)) {
                XqWorkOrderDO exists = workOrderMapper.selectActiveBySkuAndPlatform(
                        item.sku, reqVO.getListingPlatformId());
                if (exists != null) {
                    throw exception(XQ_WORK_ORDER_ALREADY_EXISTS, item.sku);
                }
            }
        }

        Map<String, Long> parentIdBySku = new LinkedHashMap<>();
        List<XqWorkOrderDO> created = new ArrayList<>();
        for (ResolvedItem item : resolved) {
            if (StrUtil.isBlank(item.sku)) {
                continue;
            }
            if (workOrderMapper.selectActiveBySkuAndPlatform(item.sku, reqVO.getListingPlatformId()) != null) {
                continue;
            }
            Long parentId = null;
            if (item.parentSku != null) {
                parentId = parentIdBySku.get(item.parentSku);
            }
            XqWorkOrderDO order = insertDispatchedOrder(reqVO, userId, item, parentId);
            applyReuseAfterInsert(order, item.sku, reqVO, userId);
            created.add(workOrderMapper.selectById(order.getId()));
            if (item.parentSku == null) {
                parentIdBySku.put(item.sku, order.getId());
            }
        }
        if (created.isEmpty()) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        for (XqWorkOrderDO order : created) {
            markSnapshotPicked(order.getExternalSku(), order.getListingPlatformId(),
                    order.getListingPlatformName());
        }
        return created;
    }

    @Override
    public List<XqWorkOrderReuseCandidateRespVO> listReuseCandidates(XqWorkOrderReuseLookupReqVO reqVO) {
        List<String> skus = CollUtil.emptyIfNull(reqVO.getSkus()).stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (skus.isEmpty()) {
            return List.of();
        }
        Set<String> excludePlatforms = new LinkedHashSet<>();
        for (String pid : CollUtil.emptyIfNull(reqVO.getExcludePlatformIds())) {
            if (StrUtil.isNotBlank(pid)) {
                excludePlatforms.add(pid.trim());
            }
        }
        List<XqWorkOrderDO> rows = workOrderMapper.selectReusableBySkus(skus);
        Map<String, XqWorkOrderReuseCandidateRespVO> bySku = new LinkedHashMap<>();
        for (String sku : skus) {
            XqWorkOrderReuseCandidateRespVO group = new XqWorkOrderReuseCandidateRespVO();
            group.setSku(sku);
            bySku.put(sku, group);
        }
        for (XqWorkOrderDO row : rows) {
            if (row == null || StrUtil.isBlank(row.getExternalSku())) {
                continue;
            }
            if (excludePlatforms.contains(StrUtil.blankToDefault(row.getListingPlatformId(), ""))) {
                continue;
            }
            XqWorkOrderReuseCandidateRespVO group = bySku.get(row.getExternalSku());
            if (group == null) {
                continue;
            }
            List<String> imageUrls = extractGeneratedImageUrls(row);
            boolean hasCopy = StrUtil.isNotBlank(row.getContentTitle());
            boolean hasImage = !imageUrls.isEmpty();
            if (!hasCopy && !hasImage) {
                continue;
            }
            XqWorkOrderReuseCandidateRespVO.Candidate c = new XqWorkOrderReuseCandidateRespVO.Candidate();
            c.setWorkOrderId(row.getId());
            c.setNo(row.getNo());
            c.setListingPlatformId(row.getListingPlatformId());
            c.setListingPlatformName(row.getListingPlatformName());
            c.setListingShopName(row.getListingShopName());
            c.setStatus(row.getStatus());
            c.setWorkflowPhase(row.getWorkflowPhase());
            c.setImageStatus(row.getImageStatus());
            c.setHasCopy(hasCopy);
            c.setHasImage(hasImage);
            c.setContentTitle(row.getContentTitle());
            c.setContentSellingPoints(row.getContentSellingPoints());
            c.setDescription(extractCopyDescription(row.getCopyResultJson()));
            c.setCoverUrl(row.getCoverUrl());
            c.setImageUrls(imageUrls);
            group.getCandidates().add(c);
        }
        return bySku.values().stream()
                .filter(g -> CollUtil.isNotEmpty(g.getCandidates()))
                .toList();
    }

    /** 下发后按用户勾选复用其它平台文案/图片 */
    private void applyReuseAfterInsert(XqWorkOrderDO order, String sku,
                                       XqWorkOrderDispatchReqVO reqVO, Long userId) {
        if (order == null || order.getId() == null || StrUtil.isBlank(sku) || reqVO == null) {
            return;
        }
        Long copyFromId = reqVO.getReuseCopyFromIds() == null ? null : reqVO.getReuseCopyFromIds().get(sku);
        Long imageFromId = reqVO.getReuseImageFromIds() == null ? null : reqVO.getReuseImageFromIds().get(sku);
        if (copyFromId == null && imageFromId == null) {
            return;
        }
        XqWorkOrderDO copySrc = copyFromId == null ? null : workOrderMapper.selectById(copyFromId);
        XqWorkOrderDO imageSrc = imageFromId == null ? null
                : (Objects.equals(imageFromId, copyFromId) ? copySrc : workOrderMapper.selectById(imageFromId));
        if (copySrc != null && !StrUtil.equals(sku, copySrc.getExternalSku())) {
            copySrc = null;
        }
        if (imageSrc != null && !StrUtil.equals(sku, imageSrc.getExternalSku())) {
            imageSrc = null;
        }
        if (copySrc == null && imageSrc == null) {
            return;
        }
        var uw = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId());
        boolean hasCopy = false;
        boolean hasImage = false;
        if (copySrc != null && StrUtil.isNotBlank(copySrc.getContentTitle())) {
            hasCopy = true;
            uw.set(XqWorkOrderDO::getContentTitle, copySrc.getContentTitle())
                    .set(XqWorkOrderDO::getContentSellingPoints, copySrc.getContentSellingPoints())
                    .set(XqWorkOrderDO::getCopyResultJson, copySrc.getCopyResultJson())
                    .set(XqWorkOrderDO::getCopyUserId, userId)
                    .set(XqWorkOrderDO::getRpaCopyStatus, "inherited")
                    .set(XqWorkOrderDO::getRpaCopyError, null)
                    .set(XqWorkOrderDO::getRpaCopyWorkUuid, null);
        }
        if (imageSrc != null) {
            List<String> urls = extractGeneratedImageUrls(imageSrc);
            if (!urls.isEmpty() || StrUtil.isNotBlank(imageSrc.getImagePromptJson())) {
                hasImage = true;
                uw.set(XqWorkOrderDO::getGeneratedImageUrl,
                                StrUtil.blankToDefault(imageSrc.getGeneratedImageUrl(), urls.isEmpty() ? null : urls.get(0)))
                        .set(XqWorkOrderDO::getImagePromptJson, imageSrc.getImagePromptJson())
                        .set(XqWorkOrderDO::getImageStatus,
                                StrUtil.blankToDefault(imageSrc.getImageStatus(), "done"))
                        .set(XqWorkOrderDO::getImageUserId, userId);
            }
        }
        if (!hasCopy && !hasImage) {
            return;
        }
        if (hasCopy && hasImage) {
            uw.set(XqWorkOrderDO::getWorkflowPhase, "list");
        } else if (hasCopy) {
            uw.set(XqWorkOrderDO::getWorkflowPhase, "image");
        }
        workOrderMapper.update(null, uw);
    }

    private static String extractCopyDescription(String copyResultJson) {
        if (StrUtil.isBlank(copyResultJson)) {
            return "";
        }
        try {
            JSONObject obj = JSONUtil.parseObj(copyResultJson);
            return StrUtil.blankToDefault(obj.getStr("description"), "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private static List<String> extractGeneratedImageUrls(XqWorkOrderDO row) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (row == null) {
            return new ArrayList<>();
        }
        if (StrUtil.isNotBlank(row.getImagePromptJson()) && !"null".equalsIgnoreCase(row.getImagePromptJson())) {
            try {
                JSONArray arr = JSONUtil.parseArray(row.getImagePromptJson());
                for (Object o : arr) {
                    if (!(o instanceof JSONObject jo)) {
                        continue;
                    }
                    String u = StrUtil.blankToDefault(jo.getStr("resultUrl"),
                            StrUtil.blankToDefault(jo.getStr("generatedImageUrl"), jo.getStr("url")));
                    if (StrUtil.isNotBlank(u) && (u.startsWith("http://") || u.startsWith("https://"))) {
                        urls.add(u.trim());
                    }
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        String gen = StrUtil.trim(row.getGeneratedImageUrl());
        if (StrUtil.isNotBlank(gen)) {
            if (gen.startsWith("[")) {
                try {
                    for (Object o : JSONUtil.parseArray(gen)) {
                        String u = StrUtil.trim(String.valueOf(o));
                        if (StrUtil.isNotBlank(u) && (u.startsWith("http://") || u.startsWith("https://"))) {
                            urls.add(u);
                        }
                    }
                } catch (Exception ignored) {
                    // ignore
                }
            } else if (gen.startsWith("http://") || gen.startsWith("https://")) {
                urls.add(gen);
            }
        }
        return new ArrayList<>(urls);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id) {
        XqWorkOrderDO order = workOrderMapper.selectById(id);
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        assertOwner(order);
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_CLOSE_INVALID);
        }
        // 关闭 + 清空文案/美工/生成图等进度，避免残留在「我的文案」
        workOrderMapper.closeAndClearProgress(id);
        unmarkSnapshotIfIdle(order.getExternalSku(), order.getListingPlatformId());
        if (order.getParentWorkOrderId() == null) {
            for (XqWorkOrderDO child : workOrderMapper.selectByParentIds(List.of(id))) {
                if (Integer.valueOf(WORK_STATUS_DOING).equals(child.getStatus())) {
                    workOrderMapper.closeAndClearProgress(child.getId());
                    unmarkSnapshotIfIdle(child.getExternalSku(),
                            StrUtil.blankToDefault(child.getListingPlatformId(), order.getListingPlatformId()));
                }
            }
        }
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
            assertOwner(order);
            if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
                continue;
            }
            workOrderMapper.closeAndClearProgress(id);
            unmarkSnapshotIfIdle(order.getExternalSku(), order.getListingPlatformId());
            if (order.getParentWorkOrderId() == null) {
                for (XqWorkOrderDO child : workOrderMapper.selectByParentIds(List.of(id))) {
                    if (Integer.valueOf(WORK_STATUS_DOING).equals(child.getStatus())) {
                        workOrderMapper.closeAndClearProgress(child.getId());
                        unmarkSnapshotIfIdle(child.getExternalSku(),
                                StrUtil.blankToDefault(child.getListingPlatformId(), order.getListingPlatformId()));
                    }
                }
            }
            count++;
        }
        return count;
    }

    @Override
    public XqWorkOrderDO generateCopy(Long id, Long userId) {
        return generateCopyInternal(id, userId);
    }

    @Override
    public long countQueuedCopyForUser(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return workOrderMapper.selectCount(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, WORK_STATUS_DOING)
                .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                .eq(XqWorkOrderDO::getRpaCopyStatus, "queued"));
    }

    @Override
    public long countQueuedCopyOnDesk(Long userId) {
        return countQueuedCopyForUser(userId);
    }

    @Override
    public long countQueuedCopyAll() {
        return workOrderMapper.selectCount(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, WORK_STATUS_DOING)
                .eq(XqWorkOrderDO::getRpaCopyStatus, "queued"));
    }

    @Override
    // 不加事务：先落库入队再调影刀；事务内触发会导致 RPA pull 时读不到未提交队列
    public int enqueueCopyRpa(XqWorkOrderCopyRpaEnqueueReqVO reqVO, Long userId) {
        if (reqVO == null || reqVO.getRootId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        XqWorkOrderDO root = validateDoing(reqVO.getRootId());
        if (root.getParentWorkOrderId() != null) {
            // 传入的是变体时，抬到主体
            Long parentId = root.getParentWorkOrderId();
            root = validateDoing(parentId);
        }
        List<XqWorkOrderDO> children = workOrderMapper.selectByParentIds(List.of(root.getId()));
        Set<Long> selected = new LinkedHashSet<>();
        selected.add(root.getId());
        if (CollUtil.isNotEmpty(reqVO.getSelectedIds())) {
            for (Long id : reqVO.getSelectedIds()) {
                if (id != null) {
                    selected.add(id);
                }
            }
        }
        // 校验 selected 都属于本家族
        Set<Long> family = new LinkedHashSet<>();
        family.add(root.getId());
        for (XqWorkOrderDO c : children) {
            if (c.getId() != null) {
                family.add(c.getId());
            }
        }
        selected.retainAll(family);

        // 落库勾选标记：主体始终 true；变体按勾选
        workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, root.getId())
                .set(XqWorkOrderDO::getCopyRpaSelected, true));
        for (XqWorkOrderDO c : children) {
            boolean on = selected.contains(c.getId());
            workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, c.getId())
                    .set(XqWorkOrderDO::getCopyRpaSelected, on));
        }

        int n = 0;
        XqWorkOrderDO triggerTarget = null;
        for (Long id : selected) {
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null || !Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
                continue;
            }
            copyPipelineService.enqueueCopyJob(order, userId);
            if (triggerTarget == null) {
                triggerTarget = order;
            }
            n += 1;
        }
        // 入队后直接调影刀控制中枢；机器人再 pull 扁平队列，无需用户手动「触发 RPA」
        if (n > 0 && triggerTarget != null) {
            copyPipelineService.triggerCopyJob(triggerTarget, userId);
        }
        return n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XqWorkOrderDO regenerateCopy(XqWorkOrderRegenerateCopyReqVO reqVO, Long userId) {
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        XqWorkOrderDO order = validateDoing(reqVO.getId());
        JSONObject copyJson;
        try {
            copyJson = StrUtil.isBlank(order.getCopyResultJson())
                    ? new JSONObject()
                    : JSONUtil.parseObj(order.getCopyResultJson());
        } catch (Exception ex) {
            copyJson = new JSONObject();
        }
        String title = StrUtil.blankToDefault(reqVO.getContentTitle(), order.getContentTitle());
        String selling = StrUtil.blankToDefault(reqVO.getContentSellingPoints(), order.getContentSellingPoints());
        String description = StrUtil.blankToDefault(
                reqVO.getContentDescription(), copyJson.getStr("description", ""));
        String revisionPrompt = StrUtil.trim(StrUtil.blankToDefault(reqVO.getRevisionPrompt(), ""));
        String target = StrUtil.blankToDefault(reqVO.getTarget(), "all").trim().toLowerCase();
        if (!Set.of("all", "title", "description", "feature").contains(target)) {
            target = "all";
        }
        Integer featureIndex = reqVO.getFeatureIndex();
        if ("feature".equals(target)) {
            if (featureIndex == null || featureIndex < 0) {
                featureIndex = 0;
            }
        } else {
            featureIndex = null;
        }

        copyJson.set("mode", "revise");
        copyJson.set("revisionPrompt", revisionPrompt);
        copyJson.set("target", target);
        if (featureIndex != null) {
            copyJson.set("featureIndex", featureIndex);
        } else {
            copyJson.remove("featureIndex");
        }
        if (StrUtil.isNotBlank(description)) {
            copyJson.set("description", description);
        }

        workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getContentTitle, StrUtil.blankToDefault(title, order.getContentTitle()))
                .set(XqWorkOrderDO::getContentSellingPoints,
                        StrUtil.blankToDefault(selling, order.getContentSellingPoints()))
                .set(XqWorkOrderDO::getCopyResultJson, copyJson.toString()));

        order = workOrderMapper.selectById(order.getId());
        copyPipelineService.enqueueCopyJob(order, userId);
        return workOrderMapper.selectById(order.getId());
    }

    @Override
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
        // 首次生成清理二次改写标记
        if (StrUtil.isNotBlank(order.getCopyResultJson())) {
            try {
                JSONObject copyJson = JSONUtil.parseObj(order.getCopyResultJson());
                if (copyJson.containsKey("mode") || copyJson.containsKey("revisionPrompt")
                        || copyJson.containsKey("target") || copyJson.containsKey("featureIndex")) {
                    copyJson.remove("mode");
                    copyJson.remove("revisionPrompt");
                    copyJson.remove("target");
                    copyJson.remove("featureIndex");
                    workOrderMapper.update(null,
                            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                                    .eq(XqWorkOrderDO::getId, id)
                                    .set(XqWorkOrderDO::getCopyResultJson, copyJson.toString()));
                    order = workOrderMapper.selectById(id);
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        copyPipelineService.enqueueCopyJob(order, claimUserId);
        return workOrderMapper.selectById(id);
    }

    private static class ResolvedItem {
        private String sku;
        private String productId;
        private String title;
        private String coverUrl;
        private String categoryName;
        private Long gigaCategoryId;
        private String variantLabel;
        private String parentSku;
        private Boolean selected;
        private XqGigaProductRow giga;
    }

    private List<ResolvedItem> expandDispatchItems(List<XqWorkOrderDispatchReqVO.Item> items) {
        List<ResolvedItem> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (XqWorkOrderDispatchReqVO.Item item : items) {
            String sku = StrUtil.trim(item.getSku());
            if (StrUtil.isBlank(sku) || !seen.add(sku)) {
                continue;
            }
            XqGigaProductRow giga = copyPipelineService.readGigaProduct(
                    StrUtil.trim(item.getProductId()), sku);
            ResolvedItem parent = fromGiga(item, sku, giga, true, null);
            out.add(parent);
            if (giga == null) {
                continue;
            }
            List<String> family = XqGigaAssociateSupport.familySkus(
                    sku, giga.getAssociateProductListJson(), giga.getAssociateProductInfoJson());
            List<String> siblingSkus = family.stream()
                    .filter(s -> !StrUtil.equals(s, sku) && seen.add(s))
                    .toList();
            if (siblingSkus.isEmpty()) {
                continue;
            }
            Map<String, XqGigaProductRow> bySku = new LinkedHashMap<>();
            for (XqGigaProductRow row : gigaProductMapper.selectListBySkus(siblingSkus)) {
                bySku.put(row.getSku(), row);
            }
            for (String sib : siblingSkus) {
                XqGigaProductRow row = bySku.get(sib);
                out.add(fromGiga(item, sib, row, false, sku));
            }
        }
        return out;
    }

    private static ResolvedItem fromGiga(XqWorkOrderDispatchReqVO.Item seed, String sku,
                                         XqGigaProductRow giga, boolean selected, String parentSku) {
        ResolvedItem it = new ResolvedItem();
        it.sku = sku;
        it.selected = selected;
        it.parentSku = parentSku;
        it.categoryName = seed.getCategoryName();
        it.gigaCategoryId = seed.getGigaCategoryId();
        it.giga = giga;
        if (giga != null) {
            it.productId = giga.getId();
            it.title = StrUtil.blankToDefault(giga.getName(), sku);
            it.coverUrl = giga.getImageUrl();
            it.variantLabel = XqGigaAssociateSupport.variantLabel(
                    sku, giga.getMainColor(), giga.getAssociateProductInfoJson());
            if (StrUtil.isNotBlank(giga.getCategoryName())) {
                it.categoryName = giga.getCategoryName();
            }
            if (giga.getGigaCategoryId() != null) {
                it.gigaCategoryId = giga.getGigaCategoryId();
            }
        } else {
            it.productId = StrUtil.trim(seed.getProductId());
            it.title = StrUtil.blankToDefault(seed.getTitle(), sku);
            it.coverUrl = seed.getCoverUrl();
            it.variantLabel = sku;
        }
        return it;
    }

    private XqWorkOrderDO insertDispatchedOrder(XqWorkOrderDispatchReqVO reqVO, Long userId,
                                                ResolvedItem item, Long parentId) {
        String title = StrUtil.blankToDefault(item.title, item.sku);
        String coverUrl = item.coverUrl;
        String sourceDescription = null;
        String sourceImageUrls = null;
        String gigaProductId = item.productId;
        XqGigaProductRow giga = item.giga;
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
            title = StrUtil.blankToDefault(giga.getName(), title);
        }
        XqWorkOrderDO order = XqWorkOrderDO.builder()
                .no("WO" + IdUtil.getSnowflakeNextIdStr())
                .sourceId(null)
                .gigaProductId(gigaProductId)
                .parentWorkOrderId(parentId)
                .parentSku(item.parentSku)
                .variantLabel(item.variantLabel)
                .externalSku(item.sku)
                .title(title)
                .coverUrl(coverUrl)
                .sourceDescription(sourceDescription)
                .sourceImageUrls(sourceImageUrls)
                .categoryName(item.categoryName)
                .gigaCategoryId(item.gigaCategoryId)
                .status(WORK_STATUS_DOING)
                .assigneeUserId(userId)
                .copyUserId(userId)
                .listingPlatformId(reqVO.getListingPlatformId())
                .listingShopId(reqVO.getListingShopId())
                .listingCategoryId(reqVO.getListingCategoryId())
                .listingPlatformName(reqVO.getListingPlatformName())
                .listingShopName(reqVO.getListingShopName())
                .listingCategoryName(reqVO.getListingCategoryName())
                .workflowPhase("copy")
                .rpaCopyStatus("idle")
                // 主体默认勾选独立跑文案；变体默认不勾选（沿用主体）
                .copyRpaSelected(parentId == null)
                .build();
        workOrderMapper.insert(order);
        return order;
    }

    /** 快照 detail_json.xqPickedPlatforms；失败不影响下发 */
    private void markSnapshotPicked(String sku, String platformId, String platformName) {
        String id = jsonSafeKey(platformId);
        if (StrUtil.isBlank(sku) || StrUtil.isBlank(id)) {
            return;
        }
        try {
            gigaProductMapper.mergePickedPlatform(sku.trim(), id,
                    StrUtil.blankToDefault(platformName, id));
        } catch (Exception ex) {
            log.warn("[markSnapshotPicked] sku={} platform={} {}", sku, id, ex.getMessage());
        }
    }

    private void unmarkSnapshotIfIdle(String sku, String platformId) {
        String id = jsonSafeKey(platformId);
        if (StrUtil.isBlank(sku) || StrUtil.isBlank(id)) {
            return;
        }
        if (workOrderMapper.selectActiveBySkuAndPlatform(sku.trim(), platformId) != null) {
            return;
        }
        try {
            gigaProductMapper.removePickedPlatform(sku.trim(), id);
        } catch (Exception ex) {
            log.warn("[unmarkSnapshotIfIdle] sku={} platform={} {}", sku, id, ex.getMessage());
        }
    }

    private static String jsonSafeKey(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        return raw.trim().replaceAll("[^0-9A-Za-z_-]", "");
    }

    public void attachVariants(List<cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (var vo : list) {
            if (vo.getId() != null) {
                ids.add(vo.getId());
            }
        }
        List<XqWorkOrderDO> children = workOrderMapper.selectByParentIds(ids);
        Map<Long, List<cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO>> grouped = new LinkedHashMap<>();
        for (XqWorkOrderDO child : children) {
            grouped.computeIfAbsent(child.getParentWorkOrderId(), k -> new ArrayList<>())
                    .add(cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(
                            child, cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO.class));
        }
        for (var vo : list) {
            vo.setVariants(grouped.getOrDefault(vo.getId(), new ArrayList<>()));
        }
        fillGigaDetail(list);
    }

    private void fillGigaDetail(
            List<cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO> list) {
        List<cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO> all = new ArrayList<>(list);
        for (var vo : list) {
            if (CollUtil.isNotEmpty(vo.getVariants())) {
                all.addAll(vo.getVariants());
            }
        }
        Set<String> skus = new LinkedHashSet<>();
        for (var vo : all) {
            if (StrUtil.isNotBlank(vo.getExternalSku())) {
                skus.add(vo.getExternalSku().trim());
            }
        }
        if (skus.isEmpty()) {
            return;
        }
        Map<String, XqGigaProductRow> byKey = new LinkedHashMap<>();
        for (XqGigaProductRow row : gigaProductMapper.selectListBySkus(skus)) {
            byKey.put(row.getSku(), row);
            if (StrUtil.isNotBlank(row.getItemCode())) {
                byKey.putIfAbsent(row.getItemCode(), row);
            }
        }
        for (var vo : all) {
            XqGigaProductRow row = byKey.get(StrUtil.trim(vo.getExternalSku()));
            if (row == null) {
                continue;
            }
            // Giga detail 里 mpn/mainColor 常为 JSON null，部分驱动会读成字面量 "null"
            String itemCode = XqGigaAssociateSupport.clean(row.getItemCode());
            String mainColor = XqGigaAssociateSupport.clean(row.getMainColor());
            if (StrUtil.isNotBlank(itemCode)) {
                vo.setItemCode(itemCode);
            }
            vo.setMainColor(mainColor);
            vo.setUpc(XqGigaAssociateSupport.clean(row.getUpc()));
            vo.setPrice(row.getPrice() != null ? row.getPrice() : row.getDiscountedPrice());
            vo.setCurrency(StrUtil.blankToDefault(row.getCurrency(), "USD"));
            vo.setQtyAvailable(row.getQtyAvailable());
            vo.setLengthCm(row.getLengthCm());
            vo.setWidthCm(row.getWidthCm());
            vo.setHeightCm(row.getHeightCm());
            if (StrUtil.isBlank(vo.getCoverUrl()) && StrUtil.isNotBlank(row.getImageUrl())) {
                vo.setCoverUrl(row.getImageUrl());
            }
            if (StrUtil.isBlank(XqGigaAssociateSupport.clean(vo.getVariantLabel()))
                    && StrUtil.isNotBlank(mainColor)) {
                vo.setVariantLabel(mainColor);
            }
        }
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
    public XqWorkOrderDO generateImage(Long id, Long userId) {
        XqWorkOrderDO order = validateDoing(id);
        assertCanAccessImage(order, userId);
        if (!isCopyReady(order)) {
            throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
        }
        imagePipelineService.enqueueImageJob(order, userId);
        return workOrderMapper.selectById(id);
    }

    @Override
    public int enqueueImageRpa(XqWorkOrderBatchIdsReqVO reqVO, Long userId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        // 先校验归属：只能入队自己可访问的图片任务，入队后仍挂在已分配美工名下
        for (Long id : reqVO.getIds()) {
            if (id == null) {
                continue;
            }
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null || !Integer.valueOf(10).equals(order.getStatus())) {
                continue;
            }
            assertCanAccessImage(order, userId);
        }
        return imagePipelineService.enqueueImageJobs(reqVO.getIds(), userId);
    }

    @Override
    public XqWorkOrderReviseImageRespVO reviseImagePreview(XqWorkOrderReviseImageReqVO reqVO, Long userId) {
        XqWorkOrderDO order = validateDoing(reqVO.getId());
        assertCanAccessImage(order, userId);

        int promptIndex = reqVO.getPromptIndex() == null ? 0 : Math.max(0, reqVO.getPromptIndex());
        String revisePrompt = StrUtil.trim(reqVO.getRevisePrompt());
        if (StrUtil.isBlank(revisePrompt)) {
            throw exception(XQ_RPA_AI_FAIL, "请填写调整说明");
        }

        String currentAiUrl = resolvePromptAiUrl(order, promptIndex);
        List<String> refs = new ArrayList<>();
        if (CollUtil.isNotEmpty(reqVO.getReferenceUrls())) {
            for (String u : reqVO.getReferenceUrls()) {
                String url = StrUtil.trim(u);
                if (StrUtil.isNotBlank(url) && !refs.contains(url)) {
                    refs.add(url);
                }
            }
        }
        boolean includeCurrentAi = Boolean.TRUE.equals(reqVO.getIncludeCurrentAi());
        if (includeCurrentAi && StrUtil.isNotBlank(currentAiUrl) && !refs.contains(currentAiUrl)) {
            // 当前 AI 作底稿时放在首位，便于上游按第一张参考改图
            refs.add(0, currentAiUrl);
        }
        if (refs.isEmpty()) {
            throw exception(XQ_RPA_AI_FAIL,
                    "请选择至少一张绑定参考图，或勾选「附带当前 AI 底稿」");
        }

        String originalPrompt = resolvePromptText(order, promptIndex);
        StringBuilder prompt = new StringBuilder();
        if (StrUtil.isNotBlank(originalPrompt)) {
            prompt.append("【原提示词】\n").append(originalPrompt).append("\n\n");
        }
        prompt.append("【调整说明】\n").append(revisePrompt);
        prompt.append("\n\n请在保持产品本体不变的前提下，按调整说明修改画面；生成结果将替换当前 AI 图位。");

        List<Map<String, Object>> images = new ArrayList<>();
        for (String url : refs) {
            Map<String, Object> img = new LinkedHashMap<>();
            img.put("url", url);
            images.add(img);
        }

        XqRpaAiGenerateImageReqVO aiReq = new XqRpaAiGenerateImageReqVO();
        aiReq.setPrompt(prompt.toString());
        aiReq.setSku(StrUtil.blankToDefault(order.getProductSku(), String.valueOf(order.getId())));
        aiReq.setWorkOrderId(order.getId());
        aiReq.setUsageCategory(XqAiUsageLogService.CAT_IMAGE_REVISE);
        aiReq.setImages(images);
        aiReq.setLockProduct(reqVO.getLockProduct() == null || Boolean.TRUE.equals(reqVO.getLockProduct()));
        aiReq.setReturnBase64(false);

        Map<String, Object> gen = rpaAiService.generateImageForConsole(aiReq);
        String previewUrl = StrUtil.trim(String.valueOf(gen.getOrDefault("url", "")));
        if (StrUtil.isBlank(previewUrl) || "null".equalsIgnoreCase(previewUrl)) {
            throw exception(XQ_RPA_AI_FAIL, "改图结果为空");
        }

        XqWorkOrderReviseImageRespVO resp = new XqWorkOrderReviseImageRespVO();
        resp.setPreviewUrl(previewUrl);
        resp.setCurrentAiUrl(currentAiUrl);
        resp.setUsedReferenceUrls(refs);
        resp.setPromptUsed(StrUtil.blankToDefault(String.valueOf(gen.getOrDefault("promptUsed", "")), prompt.toString()));
        resp.setModel(StrUtil.blankToDefault(String.valueOf(gen.getOrDefault("model", "")), null));
        Object cost = gen.get("costMs");
        if (cost instanceof Number n) {
            resp.setCostMs(n.longValue());
        }
        return resp;
    }

    private void assertCanAccessImage(XqWorkOrderDO order, Long userId) {
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (userId == null) {
            throw exception(XQ_WORK_ORDER_ACCESS_DENIED);
        }
        if (Objects.equals(order.getImageUserId(), userId)
                || Objects.equals(order.getCopyUserId(), userId)
                || Objects.equals(order.getAssigneeUserId(), userId)
                || order.getImageUserId() == null) {
            return;
        }
        throw exception(XQ_WORK_ORDER_ACCESS_DENIED);
    }

    private String resolvePromptAiUrl(XqWorkOrderDO order, int promptIndex) {
        if (order == null) {
            return "";
        }
        try {
            JSONArray arr = JSONUtil.parseArray(StrUtil.blankToDefault(order.getImagePromptJson(), "[]"));
            if (promptIndex >= 0 && promptIndex < arr.size()) {
                JSONObject item = arr.getJSONObject(promptIndex);
                if (item != null) {
                    String url = StrUtil.blankToDefault(item.getStr("resultUrl"), item.getStr("generatedImageUrl"));
                    if (StrUtil.isNotBlank(url)) {
                        return url.trim();
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return StrUtil.blankToDefault(order.getGeneratedImageUrl(), "");
    }

    private String resolvePromptText(XqWorkOrderDO order, int promptIndex) {
        if (order == null || StrUtil.isBlank(order.getImagePromptJson())) {
            return "";
        }
        try {
            JSONArray arr = JSONUtil.parseArray(order.getImagePromptJson());
            if (promptIndex >= 0 && promptIndex < arr.size()) {
                JSONObject item = arr.getJSONObject(promptIndex);
                if (item != null) {
                    return StrUtil.blankToDefault(item.getStr("promptText"), "").trim();
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "";
    }

    @Override
    public List<XqAssignableImageUserRespVO> listAssignableImageUsers(Long operatorUserId) {
        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> permUserIds = permissionApi.getUserIdListByPermission(PERM_XQ_IMAGE);
        if (CollUtil.isNotEmpty(permUserIds)) {
            userIds.addAll(permUserIds);
        }
        if (operatorUserId != null) {
            userIds.add(operatorUserId);
        }
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<XqAssignableImageUserRespVO> out = new ArrayList<>();
        for (AdminUserRespDTO user : adminUserApi.getUserList(userIds)) {
            if (user == null || user.getId() == null) {
                continue;
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                continue;
            }
            XqAssignableImageUserRespVO row = new XqAssignableImageUserRespVO();
            row.setId(user.getId());
            row.setNickname(user.getNickname());
            out.add(row);
        }
        out.sort((a, b) -> {
            if (Objects.equals(a.getId(), operatorUserId)) {
                return -1;
            }
            if (Objects.equals(b.getId(), operatorUserId)) {
                return 1;
            }
            return String.valueOf(a.getNickname()).compareTo(String.valueOf(b.getNickname()));
        });
        return out;
    }

    private void validateAssignableImageUser(Long imageUserId, Long operatorUserId) {
        if (imageUserId == null) {
            throw exception(XQ_WORK_ORDER_IMAGE_USER_INVALID);
        }
        if (operatorUserId != null && imageUserId.equals(operatorUserId)) {
            return;
        }
        if (permissionApi.hasAnyPermissions(imageUserId, PERM_XQ_IMAGE)) {
            return;
        }
        throw exception(XQ_WORK_ORDER_IMAGE_USER_INVALID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAssignImage(XqWorkOrderAssignImageReqVO reqVO, Long operatorUserId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        validateAssignableImageUser(reqVO.getImageUserId(), operatorUserId);
        // 外侧默认整家族；编辑页勾选分配传 expandFamily=false
        boolean expand = reqVO.getExpandFamily() == null || Boolean.TRUE.equals(reqVO.getExpandFamily());
        LinkedHashSet<Long> expandIds = expand
                ? expandAssignImageFamilyIds(reqVO.getIds())
                : new LinkedHashSet<>(reqVO.getIds().stream().filter(Objects::nonNull).toList());
        Map<Long, XqWorkOrderDO> cache = new LinkedHashMap<>();
        int count = 0;
        for (Long id : expandIds) {
            XqWorkOrderDO order = validateDoing(id);
            cache.put(id, order);
            // 变体无独立文案时从主体沿用；纯分组父级无文案则跳过
            if (!ensureCopyReadyForAssign(order, cache)) {
                continue;
            }
            if (!canOperatorAssignImage(order, cache, operatorUserId)) {
                continue;
            }
            // 美工已开跑（执行中/已出图）不可再改派
            if (order.getImageUserId() != null && isImageExecutionStarted(order)) {
                continue;
            }
            XqWorkOrderDO update = new XqWorkOrderDO();
            update.setId(id);
            update.setImageUserId(reqVO.getImageUserId());
            boolean hasPrompt = StrUtil.isNotBlank(order.getImagePromptJson())
                    && !"[]".equals(order.getImagePromptJson().trim());
            // 改派时保持未执行状态，不把 running 重置掉（上面已跳过已开跑）
            update.setImageStatus(hasPrompt ? "pending" : "todo");
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
        if (count == 0) {
            throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindListing(XqWorkOrderBindListingReqVO reqVO, Long operatorUserId) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getIds())) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        if (StrUtil.isBlank(reqVO.getListingPlatformId()) || StrUtil.isBlank(reqVO.getListingCategoryId())) {
            throw exception(XQ_DISPATCH_LISTING_REQUIRED);
        }
        if (storeScopeService.getPlatform(XqStoreScopeService.parseLong(reqVO.getListingPlatformId())) == null) {
            throw exception(XQ_PLATFORM_NOT_EXISTS);
        }
        if (StrUtil.isNotBlank(reqVO.getListingShopId())) {
            Long shopId = XqStoreScopeService.parseLong(reqVO.getListingShopId());
            if (storeScopeService.getStore(shopId) == null) {
                throw exception(XQ_STORE_NOT_EXISTS);
            }
            if (operatorUserId != null && !storeScopeService.isStoreAllowed(operatorUserId, shopId)) {
                throw exception(XQ_STORE_ACCESS_DENIED);
            }
        }
        boolean expand = reqVO.getExpandFamily() == null || Boolean.TRUE.equals(reqVO.getExpandFamily());
        LinkedHashSet<Long> ids = expand
                ? expandAssignImageFamilyIds(reqVO.getIds())
                : new LinkedHashSet<>(reqVO.getIds().stream().filter(Objects::nonNull).toList());
        int count = 0;
        for (Long id : ids) {
            XqWorkOrderDO order = validateDoing(id);
            String sku = StrUtil.blankToDefault(order.getExternalSku(), "").trim();
            if (StrUtil.isNotBlank(sku)) {
                XqWorkOrderDO conflict = workOrderMapper.selectActiveBySkuAndPlatform(
                        sku, reqVO.getListingPlatformId());
                if (conflict != null && !Objects.equals(conflict.getId(), order.getId())
                        && !ids.contains(conflict.getId())) {
                    throw exception(XQ_WORK_ORDER_ALREADY_EXISTS, sku);
                }
            }
            XqWorkOrderDO update = new XqWorkOrderDO();
            update.setId(id);
            update.setListingPlatformId(reqVO.getListingPlatformId().trim());
            update.setListingPlatformName(StrUtil.trim(reqVO.getListingPlatformName()));
            update.setListingCategoryId(reqVO.getListingCategoryId().trim());
            update.setListingCategoryName(StrUtil.trim(reqVO.getListingCategoryName()));
            if (StrUtil.isNotBlank(reqVO.getListingShopId())) {
                update.setListingShopId(reqVO.getListingShopId().trim());
                update.setListingShopName(StrUtil.trim(reqVO.getListingShopName()));
            }
            // 已有图文时，补绑后进入可上架/待审链路
            if ("done".equals(order.getImageStatus())
                    || "generated".equals(order.getImageStatus())
                    || "revised".equals(order.getImageStatus())) {
                update.setWorkflowPhase(StrUtil.blankToDefault(order.getWorkflowPhase(), "list"));
            }
            workOrderMapper.updateById(update);
            count++;
        }
        return count;
    }

    /** 将请求 id 扩展为「主体 + 全部变体」 */
    private LinkedHashSet<Long> expandAssignImageFamilyIds(List<Long> ids) {
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        LinkedHashSet<Long> rootIds = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null) {
                continue;
            }
            Long rootId = order.getParentWorkOrderId() != null
                    ? order.getParentWorkOrderId() : order.getId();
            rootIds.add(rootId);
            out.add(rootId);
            out.add(order.getId());
        }
        if (!rootIds.isEmpty()) {
            List<XqWorkOrderDO> children = workOrderMapper.selectByParentIds(rootIds);
            for (XqWorkOrderDO child : children) {
                if (child != null && child.getId() != null) {
                    out.add(child.getId());
                }
            }
        }
        return out;
    }

    /**
     * 变体缺文案时从主体或已有文案的兄弟变体沿用。
     * @return false 表示无法分配（如纯分组父级自身无文案），调用方跳过
     */
    private boolean ensureCopyReadyForAssign(XqWorkOrderDO order, Map<Long, XqWorkOrderDO> cache) {
        if (isCopyReady(order)) {
            return true;
        }
        Long parentId = order.getParentWorkOrderId();
        if (parentId == null) {
            return false;
        }
        XqWorkOrderDO source = cache.get(parentId);
        if (source == null) {
            source = workOrderMapper.selectById(parentId);
            if (source != null) {
                cache.put(parentId, source);
            }
        }
        if (source == null || !isCopyReady(source)) {
            source = null;
            for (XqWorkOrderDO sib : workOrderMapper.selectByParentIds(List.of(parentId))) {
                if (sib == null || sib.getId() == null || sib.getId().equals(order.getId())) {
                    continue;
                }
                if (isCopyReady(sib)) {
                    source = sib;
                    cache.put(sib.getId(), sib);
                    break;
                }
            }
        }
        if (source == null) {
            return false;
        }
        workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getContentTitle, source.getContentTitle())
                .set(XqWorkOrderDO::getContentSellingPoints, source.getContentSellingPoints())
                .set(XqWorkOrderDO::getCopyResultJson, source.getCopyResultJson())
                .set(XqWorkOrderDO::getImagePromptJson, source.getImagePromptJson())
                .set(XqWorkOrderDO::getRpaCopyStatus,
                        StrUtil.blankToDefault(order.getRpaCopyStatus(), "inherited"))
                .set(XqWorkOrderDO::getWorkflowPhase, "image")
                .set(order.getCopyUserId() == null, XqWorkOrderDO::getCopyUserId, source.getCopyUserId()));
        XqWorkOrderDO refreshed = workOrderMapper.selectById(order.getId());
        if (refreshed == null || !isCopyReady(refreshed)) {
            return false;
        }
        cache.put(order.getId(), refreshed);
        order.setContentTitle(refreshed.getContentTitle());
        order.setContentSellingPoints(refreshed.getContentSellingPoints());
        order.setCopyResultJson(refreshed.getCopyResultJson());
        order.setImagePromptJson(refreshed.getImagePromptJson());
        order.setCopyUserId(refreshed.getCopyUserId());
        order.setRpaCopyStatus(refreshed.getRpaCopyStatus());
        return true;
    }

    /** 本人文案/下发，或变体跟家族主体同属本人，可分配 */
    private boolean canOperatorAssignImage(XqWorkOrderDO order, Map<Long, XqWorkOrderDO> cache,
                                           Long operatorUserId) {
        if (operatorUserId == null) {
            return true;
        }
        if (order.getCopyUserId() == null || operatorUserId.equals(order.getCopyUserId())
                || operatorUserId.equals(order.getAssigneeUserId())) {
            return true;
        }
        Long parentId = order.getParentWorkOrderId();
        if (parentId == null) {
            return false;
        }
        XqWorkOrderDO parent = cache.get(parentId);
        if (parent == null) {
            parent = workOrderMapper.selectById(parentId);
            if (parent != null) {
                cache.put(parentId, parent);
            }
        }
        return parent != null && (operatorUserId.equals(parent.getCopyUserId())
                || operatorUserId.equals(parent.getAssigneeUserId())
                || String.valueOf(operatorUserId).equals(StrUtil.blankToDefault(parent.getCreator(), "")));
    }

    @Override
    public void updateImageStatus(XqWorkOrderImageStatusReqVO reqVO) {
        XqWorkOrderDO order = validateDoing(reqVO.getId());
        if (order.getImageUserId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_ASSIGNED_IMAGE);
        }
        String status = normalizeImageStatus(reqVO.getImageStatus());
        if (status == null) {
            throw exception(XQ_WORK_ORDER_IMAGE_STATUS_INVALID);
        }
        if ("done".equals(status)) {
            // 审核确认：须图片与文案都齐全，才进入待上架
            if (!isImageReady(order)) {
                throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
            }
            if (!isCopyReady(order)) {
                throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
            }
        }
        if ("generated".equals(status) && !isImageReady(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        String rejectReason = StrUtil.trim(reqVO.getRejectReason());
        if ("rejected".equals(status) && StrUtil.isBlank(rejectReason)) {
            throw exception(XQ_WORK_ORDER_REJECT_REASON_REQUIRED);
        }
        // 撤回到待执行：清掉生图 RPA 执行态与驳回原因，便于重新入队
        if ("pending".equals(status) || "todo".equals(status)) {
            workOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, order.getId())
                    .set(XqWorkOrderDO::getImageStatus, status)
                    .set(XqWorkOrderDO::getImageRejectReason, null)
                    .set(XqWorkOrderDO::getImagePromptJson, clearPromptRejectReasons(order.getImagePromptJson()))
                    .set(XqWorkOrderDO::getWorkflowPhase, "image")
                    .set(XqWorkOrderDO::getRpaImageStatus, null)
                    .set(XqWorkOrderDO::getRpaImageError, null)
                    .set(XqWorkOrderDO::getRpaImageWorkUuid, null));
            return;
        }
        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setImageStatus(status);
        update.setWorkflowPhase("done".equals(status) ? "list" : "image");
        if ("rejected".equals(status)) {
            String json = applyPromptRejectReason(order.getImagePromptJson(), reqVO.getRejectIndex(), rejectReason);
            update.setImagePromptJson(json);
            update.setImageRejectReason(StrUtil.maxLength(summarizePromptRejectReasons(json), 500));
        } else if ("generated".equals(status) || "done".equals(status)) {
            update.setImageRejectReason(null);
            update.setImagePromptJson(clearPromptRejectReasons(order.getImagePromptJson()));
        } else if ("revised".equals(status)) {
            // 美工确认已改：按槽位清驳回；未传槽位则清全部。若仍有其它驳回则保持 rejected
            Integer fixIndex = reqVO.getRejectIndex();
            String json = fixIndex != null && fixIndex >= 0
                    ? clearPromptRejectReasonAt(order.getImagePromptJson(), fixIndex)
                    : clearPromptRejectReasons(order.getImagePromptJson());
            update.setImagePromptJson(json);
            String summary = summarizePromptRejectReasons(json);
            if (StrUtil.isNotBlank(summary)) {
                update.setImageRejectReason(StrUtil.maxLength(summary, 500));
                update.setImageStatus("rejected");
            } else {
                update.setImageRejectReason(null);
            }
        }
        workOrderMapper.updateById(update);
    }

    /** 把驳回原因写入指定槽位；未指定槽位则写入所有已出图槽位 */
    private static String applyPromptRejectReason(String imagePromptJson, Integer rejectIndex, String reason) {
        String text = StrUtil.maxLength(StrUtil.trim(reason), 200);
        JSONArray arr;
        try {
            arr = JSONUtil.parseArray(StrUtil.blankToDefault(imagePromptJson, "[]"));
        } catch (Exception ex) {
            arr = new JSONArray();
        }
        if (rejectIndex != null && rejectIndex >= 0) {
            while (arr.size() <= rejectIndex) {
                JSONObject pad = new JSONObject();
                pad.set("index", arr.size());
                arr.add(pad);
            }
            JSONObject item = arr.getJSONObject(rejectIndex);
            if (item == null) {
                item = new JSONObject();
                item.set("index", rejectIndex);
                arr.set(rejectIndex, item);
            }
            item.set("rejectReason", text);
        } else {
            if (arr.isEmpty()) {
                JSONObject item = new JSONObject();
                item.set("index", 0);
                item.set("rejectReason", text);
                arr.add(item);
            } else {
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String url = StrUtil.blankToDefault(item.getStr("resultUrl"), item.getStr("generatedImageUrl"));
                    if (StrUtil.isNotBlank(url)) {
                        item.set("rejectReason", text);
                    }
                }
            }
        }
        return arr.toString();
    }

    private static String clearPromptRejectReasons(String imagePromptJson) {
        if (StrUtil.isBlank(imagePromptJson)) {
            return imagePromptJson;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(imagePromptJson);
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item != null) {
                    item.remove("rejectReason");
                }
            }
            return arr.toString();
        } catch (Exception ex) {
            return imagePromptJson;
        }
    }

    /** 清除指定槽位的驳回原因 */
    private static String clearPromptRejectReasonAt(String imagePromptJson, int rejectIndex) {
        if (StrUtil.isBlank(imagePromptJson) || rejectIndex < 0) {
            return imagePromptJson;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(imagePromptJson);
            if (rejectIndex < arr.size()) {
                JSONObject item = arr.getJSONObject(rejectIndex);
                if (item != null) {
                    item.remove("rejectReason");
                }
            }
            return arr.toString();
        } catch (Exception ex) {
            return imagePromptJson;
        }
    }

    private static String summarizePromptRejectReasons(String imagePromptJson) {
        if (StrUtil.isBlank(imagePromptJson)) {
            return "";
        }
        try {
            JSONArray arr = JSONUtil.parseArray(imagePromptJson);
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                String reason = StrUtil.trim(item.getStr("rejectReason"));
                if (StrUtil.isBlank(reason)) {
                    continue;
                }
                parts.add("图" + (i + 1) + "：" + reason);
            }
            return String.join("；", parts);
        } catch (Exception ex) {
            return "";
        }
    }

    @Override
    public Long listWorkOrder(XqWorkOrderListReqVO reqVO) {
        // 导表路径会读模板/规则表（已在 master），长耗时导出不宜套外层事务
        XqWorkOrderDO order = validateDoing(reqVO.getId());
        if (!isListableImage(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        Map<String, String> values =
                reqVO.getValues() == null ? Map.of() : reqVO.getValues();
        XqListingModeHelper.PlatformListingMeta meta =
                listingModeHelper.resolveBySourceOrBusinessId(order.getListingPlatformId());
        String requested = StrUtil.blankToDefault(reqVO.getListingMode(), meta.listingMode())
                .trim().toLowerCase();
        if (!requested.equals(meta.listingMode())) {
            throw exception(XQ_LISTING_MODE_MISMATCH,
                    XqListingModeHelper.MODE_EXPORT.equals(meta.listingMode()) ? "导表" : "API");
        }

        XqWorkOrderDO snap = new XqWorkOrderDO();
        snap.setId(order.getId());
        snap.setListingValuesJson(JSONUtil.toJsonStr(values));
        snap.setWorkflowPhase("list");

        if (XqListingModeHelper.MODE_EXPORT.equals(requested)) {
            XqListingExportResult exported = listingSplitExportService.exportWorkOrder(order, values);
            snap.setListingStatus("export_pending_confirm");
            snap.setListingResultJson(JSONUtil.toJsonStr(Map.of(
                    "ok", true,
                    "mode", "export",
                    "filename", exported.getFilename(),
                    "productCount", exported.getProductCount(),
                    "message", "导表成功，待确认上架")));
            workOrderMapper.updateById(snap);
            return null;
        }

        Map<String, Object> apiResult = publishViaApi(order, meta, values);
        snap.setListingResultJson(JSONUtil.toJsonStr(apiResult));
        workOrderMapper.updateById(snap);

        XqWorkOrderCompleteReqVO complete = new XqWorkOrderCompleteReqVO();
        complete.setId(order.getId());
        complete.setProductSku(StrUtil.blankToDefault(reqVO.getProductSku(),
                StrUtil.blankToDefault(order.getExternalSku(), "SKU" + order.getId())));
        complete.setProductName(StrUtil.blankToDefault(reqVO.getProductName(),
                StrUtil.blankToDefault(order.getContentTitle(), order.getTitle())));
        complete.setCategoryName(order.getListingCategoryName());
        Long productId = completeWorkOrder(complete);

        XqWorkOrderDO result = new XqWorkOrderDO();
        result.setId(order.getId());
        result.setListingStatus("listed");
        result.setListingResultJson(JSONUtil.toJsonStr(apiResult));
        workOrderMapper.updateById(result);
        return productId;
    }

    @Override
    public XqListingExportResult exportListing(XqWorkOrderListReqVO reqVO) {
        // 不在此开主库事务：导表要读 xq 库模板，@Transactional 会绑死 master 连接
        XqWorkOrderDO order = validateDoing(reqVO.getId());
        if (!isListableImage(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        Map<String, String> values =
                reqVO.getValues() == null ? Map.of() : reqVO.getValues();
        XqListingModeHelper.PlatformListingMeta meta =
                listingModeHelper.resolveBySourceOrBusinessId(order.getListingPlatformId());
        if (!XqListingModeHelper.MODE_EXPORT.equals(meta.listingMode())) {
            throw exception(XQ_LISTING_MODE_MISMATCH, "API");
        }
        XqListingExportResult exported = listingSplitExportService.exportWorkOrder(order, values);
        XqWorkOrderDO snap = new XqWorkOrderDO();
        snap.setId(order.getId());
        snap.setListingValuesJson(JSONUtil.toJsonStr(values));
        snap.setWorkflowPhase("list");
        snap.setListingStatus("export_pending_confirm");
        snap.setListingResultJson(JSONUtil.toJsonStr(Map.of(
                "ok", true,
                "mode", "export",
                "filename", exported.getFilename(),
                "productCount", exported.getProductCount(),
                "message", "导表成功，待确认上架")));
        workOrderMapper.updateById(snap);
        return exported;
    }

    private Map<String, Object> publishViaApi(
            XqWorkOrderDO order,
            XqListingModeHelper.PlatformListingMeta meta,
            Map<String, String> values) {
        String provider = StrUtil.blankToDefault(meta.apiProvider(), "mirakl");
        XqListingPlatformCredentialDO cred = null;
        if (meta.platformId() != null) {
            cred = listingCredentialService.getDecryptedForPlatform(meta.platformId(), provider);
        }
        if (cred == null && StrUtil.isNotBlank(meta.sourcePlatformId())) {
            cred = listingCredentialService.getDecryptedBySource(meta.sourcePlatformId(), provider);
        }
        if (cred == null) {
            throw exception(XQ_LISTING_CREDENTIAL_REQUIRED);
        }
        if (XqListingPlatformCredentialDO.PROVIDER_WAYFAIR.equals(provider)) {
            return wayfairListingClient.publishProduct(cred, order, values);
        }
        return miraklListingClient.publishProduct(cred, order, values);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmListing(Long id) {
        XqWorkOrderDO order = validateDoing(id);
        if (!"export_pending_confirm".equals(StrUtil.blankToDefault(order.getListingStatus(), ""))) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        XqWorkOrderCompleteReqVO complete = new XqWorkOrderCompleteReqVO();
        complete.setId(order.getId());
        complete.setProductSku(StrUtil.blankToDefault(order.getExternalSku(), "SKU" + order.getId()));
        complete.setProductName(StrUtil.blankToDefault(order.getContentTitle(), order.getTitle()));
        complete.setCategoryName(order.getListingCategoryName());
        Long productId = completeWorkOrder(complete);
        XqWorkOrderDO result = new XqWorkOrderDO();
        result.setId(order.getId());
        result.setListingStatus("listed");
        result.setListingResultJson("{\"ok\":true,\"mode\":\"export\",\"message\":\"用户确认已上架\"}");
        workOrderMapper.updateById(result);
        return productId;
    }

    private static boolean isListableImage(XqWorkOrderDO order) {
        String s = StrUtil.blankToDefault(order.getImageStatus(), "");
        return "done".equals(s) || "generated".equals(s) || "revised".equals(s);
    }

    /** 美工是否已开始执行（开始后不可改派） */
    private static boolean isImageExecutionStarted(XqWorkOrderDO order) {
        String s = StrUtil.blankToDefault(order.getImageStatus(), "").toLowerCase();
        return "running".equals(s) || "generated".equals(s) || "revised".equals(s)
                || "rejected".equals(s) || "done".equals(s);
    }

    private static String normalizeImageStatus(String raw) {
        String s = StrUtil.blankToDefault(raw, "").trim().toLowerCase();
        if ("todo".equals(s) || "pending".equals(s) || "running".equals(s)
                || "generated".equals(s) || "rejected".equals(s)
                || "revised".equals(s) || "done".equals(s)) {
            return s;
        }
        return null;
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
        assertOwner(order);
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        return order;
    }

    /** 下发人 / 文案领取人 / 美工 / 创建人 才可看自己的任务 */
    private void assertOwner(XqWorkOrderDO order) {
        Long uid = getLoginUserId();
        if (isBoundUser(order, uid)) {
            return;
        }
        if (order.getParentWorkOrderId() != null) {
            XqWorkOrderDO parent = workOrderMapper.selectById(order.getParentWorkOrderId());
            if (parent != null && isBoundUser(parent, uid)) {
                return;
            }
        }
        throw exception(XQ_WORK_ORDER_ACCESS_DENIED);
    }

    private static boolean isBoundUser(XqWorkOrderDO order, Long uid) {
        if (order == null || uid == null) {
            return false;
        }
        return uid.equals(order.getAssigneeUserId())
                || uid.equals(order.getCopyUserId())
                || uid.equals(order.getImageUserId())
                || String.valueOf(uid).equals(StrUtil.blankToDefault(order.getCreator(), ""));
    }

    private static boolean isCopyReady(XqWorkOrderDO order) {
        return StrUtil.isNotBlank(order.getContentTitle());
    }

    private static boolean isImageReady(XqWorkOrderDO order) {
        return StrUtil.isNotBlank(order.getGeneratedImageUrl());
    }

}
