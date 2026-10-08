package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
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
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReuseCandidateRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReuseLookupReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqProductDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
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
    private XqGigaProductMapper gigaProductMapper;
    @Resource
    private XqStoreScopeService storeScopeService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    public PageResult<XqWorkOrderDO> getWorkOrderPage(XqWorkOrderPageReqVO pageReqVO) {
        return workOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public XqWorkOrderDO getWorkOrder(Long id) {
        XqWorkOrderDO order = workOrderMapper.selectById(id);
        if (order == null) {
            return null;
        }
        assertOwner(order);
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
        if (order.getParentWorkOrderId() == null) {
            for (XqWorkOrderDO child : workOrderMapper.selectByParentIds(List.of(id))) {
                if (Integer.valueOf(WORK_STATUS_DOING).equals(child.getStatus())) {
                    workOrderMapper.closeAndClearProgress(child.getId());
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
            if (order.getParentWorkOrderId() == null) {
                for (XqWorkOrderDO child : workOrderMapper.selectByParentIds(List.of(id))) {
                    if (Integer.valueOf(WORK_STATUS_DOING).equals(child.getStatus())) {
                        workOrderMapper.closeAndClearProgress(child.getId());
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
    @Transactional(rollbackFor = Exception.class)
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
        for (Long id : selected) {
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null || !Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
                continue;
            }
            copyPipelineService.enqueueCopyJob(order, userId);
            n += 1;
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
            vo.setItemCode(StrUtil.blankToDefault(row.getItemCode(), vo.getItemCode()));
            vo.setMainColor(row.getMainColor());
            vo.setUpc(row.getUpc());
            vo.setPrice(row.getPrice() != null ? row.getPrice() : row.getDiscountedPrice());
            vo.setCurrency(StrUtil.blankToDefault(row.getCurrency(), "USD"));
            vo.setQtyAvailable(row.getQtyAvailable());
            vo.setLengthCm(row.getLengthCm());
            vo.setWidthCm(row.getWidthCm());
            vo.setHeightCm(row.getHeightCm());
            if (StrUtil.isBlank(vo.getCoverUrl()) && StrUtil.isNotBlank(row.getImageUrl())) {
                vo.setCoverUrl(row.getImageUrl());
            }
            if (StrUtil.isBlank(vo.getVariantLabel()) && StrUtil.isNotBlank(row.getMainColor())) {
                vo.setVariantLabel(row.getMainColor());
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
    public XqWorkOrderDO generateImage(Long id) {
        XqWorkOrderDO order = validateDoing(id);
        // 原逻辑：必须先文案，再图片
        if (!isCopyReady(order)) {
            throw exception(XQ_WORK_ORDER_COPY_REQUIRED);
        }
        // 进入图片执行中
        XqWorkOrderDO running = new XqWorkOrderDO();
        running.setId(order.getId());
        running.setImageStatus("running");
        running.setWorkflowPhase("image");
        workOrderMapper.updateById(running);

        String image = StrUtil.blankToDefault(order.getCoverUrl(), "");
        if (StrUtil.isBlank(image)) {
            image = "https://via.placeholder.com/800x800.png?text=" + order.getExternalSku();
        }
        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setGeneratedImageUrl(image);
        update.setImageStatus("generated");
        update.setWorkflowPhase("image");
        workOrderMapper.updateById(update);
        return workOrderMapper.selectById(id);
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
        if ("done".equals(status) && !isImageReady(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        if ("generated".equals(status) && !isImageReady(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        XqWorkOrderDO update = new XqWorkOrderDO();
        update.setId(order.getId());
        update.setImageStatus(status);
        update.setWorkflowPhase("done".equals(status) ? "list" : "image");
        workOrderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long listWorkOrder(XqWorkOrderListReqVO reqVO) {
        XqWorkOrderDO order = validateDoing(reqVO.getId());
        if (!isListableImage(order)) {
            throw exception(XQ_WORK_ORDER_IMAGE_REQUIRED);
        }
        java.util.Map<String, String> values =
                reqVO.getValues() == null ? java.util.Map.of() : reqVO.getValues();
        String mode = StrUtil.blankToDefault(reqVO.getListingMode(), "api").trim().toLowerCase();

        XqWorkOrderDO snap = new XqWorkOrderDO();
        snap.setId(order.getId());
        snap.setListingValuesJson(JSONUtil.toJsonStr(values));
        snap.setWorkflowPhase("list");

        // 无 API：导表成功 → 待用户确认是否上架
        if ("export".equals(mode)) {
            snap.setListingStatus("export_pending_confirm");
            snap.setListingResultJson("{\"ok\":true,\"mode\":\"export\",\"message\":\"导表成功，待确认上架\"}");
            workOrderMapper.updateById(snap);
            return null;
        }

        // 有 API：程序提交并判定成功上架
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
        result.setListingResultJson("{\"ok\":true,\"mode\":\"api\",\"message\":\"API 上架成功\"}");
        workOrderMapper.updateById(result);
        return productId;
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
