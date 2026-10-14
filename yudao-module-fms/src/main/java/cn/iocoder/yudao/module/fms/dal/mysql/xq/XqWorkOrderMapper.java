package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Mapper
public interface XqWorkOrderMapper extends BaseMapperX<XqWorkOrderDO> {

    default PageResult<XqWorkOrderDO> selectPage(XqWorkOrderPageReqVO reqVO) {
        List<Long> childParentIds = Collections.emptyList();
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            childParentIds = selectParentIdsByChildKeyword(reqVO.getKeyword().trim());
        }
        // 图片池：扁平拉「相关家族全部 SKU」，UI 不区分主/变体
        boolean imagePoolFlat = reqVO.getMineImageUserId() != null;
        boolean selectedPool = Boolean.TRUE.equals(reqVO.getSelectedPool());
        Long imageArtistId = null;
        if (imagePoolFlat) {
            imageArtistId = reqVO.getImageUserId() != null
                    ? reqVO.getImageUserId() : reqVO.getMineImageUserId();
        }
        LambdaQueryWrapperX<XqWorkOrderDO> wrapper = new LambdaQueryWrapperX<XqWorkOrderDO>()
                .likeIfPresent(XqWorkOrderDO::getNo, reqVO.getNo())
                .likeIfPresent(XqWorkOrderDO::getTitle, reqVO.getTitle())
                .likeIfPresent(XqWorkOrderDO::getExternalSku, reqVO.getExternalSku())
                .eqIfPresent(XqWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(XqWorkOrderDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                .eqIfPresent(XqWorkOrderDO::getCopyUserId, reqVO.getCopyUserId())
                // 图片池用家族展开，避免只 eq 美工时漏掉未单独写 imageUserId 的变体行
                .eqIfPresent(XqWorkOrderDO::getImageUserId, imagePoolFlat ? null : reqVO.getImageUserId())
                .eqIfPresent(XqWorkOrderDO::getWorkflowPhase, reqVO.getWorkflowPhase())
                .eqIfPresent(XqWorkOrderDO::getListingPlatformId, reqVO.getListingPlatformId())
                .eqIfPresent(XqWorkOrderDO::getListingShopId, reqVO.getListingShopId())
                .eqIfPresent(XqWorkOrderDO::getListingCategoryId, reqVO.getListingCategoryId());
        applyImageStatusFilter(wrapper, reqVO.getImageStatus());
        if (reqVO.getGigaCategoryIds() != null && !reqVO.getGigaCategoryIds().isEmpty()) {
            wrapper.in(XqWorkOrderDO::getGigaCategoryId, reqVO.getGigaCategoryIds());
        } else {
            wrapper.eqIfPresent(XqWorkOrderDO::getGigaCategoryId, reqVO.getGigaCategoryId());
        }
        if (Boolean.TRUE.equals(reqVO.getListingCategorySelected())) {
            wrapper.isNotNull(XqWorkOrderDO::getListingCategoryId)
                    .ne(XqWorkOrderDO::getListingCategoryId, "");
        } else if (Boolean.FALSE.equals(reqVO.getListingCategorySelected())) {
            wrapper.and(w -> w.isNull(XqWorkOrderDO::getListingCategoryId)
                    .or()
                    .eq(XqWorkOrderDO::getListingCategoryId, ""));
        }
        if (selectedPool) {
            applySelectedPoolFilter(wrapper);
        }
        applyFlowStatus(wrapper, reqVO.getFlowStatus());
        // 已选产品池给运营看全量旧数据，不按认领人收窄
        if (reqVO.getBoundUserId() != null && !imagePoolFlat && !selectedPool) {
            // 文案/任务工作台：下发时已经按认领人切开，列表不再按文案池二次筛选
            wrapper.eq(XqWorkOrderDO::getAssigneeUserId, reqVO.getBoundUserId());
        }
        if (reqVO.getMineUserId() != null) {
            wrapper.eq(XqWorkOrderDO::getAssigneeUserId, reqVO.getMineUserId());
        }
        if (imagePoolFlat && imageArtistId != null) {
            applyImagePoolFamilyFilter(wrapper, imageArtistId);
            // 未选平台的旧数据集中到「已选产品池」补绑，不占编辑图片列表
            if (reqVO.getListingPlatformId() == null || reqVO.getListingPlatformId().isBlank()) {
                wrapper.isNotNull(XqWorkOrderDO::getListingPlatformId)
                        .ne(XqWorkOrderDO::getListingPlatformId, "");
            }
        }
        if (Boolean.TRUE.equals(reqVO.getListingReady())) {
            // 须审核确认图文（imageStatus=done）后才可上架
            wrapper.eq(XqWorkOrderDO::getImageStatus, "done");
        }
        // 文案池/任务列表仍只出主体；图片池出扁平全量 SKU
        if (!imagePoolFlat) {
            wrapper.isNull(XqWorkOrderDO::getParentWorkOrderId);
        }
        if (Boolean.TRUE.equals(reqVO.getCopyReady())) {
            wrapper.isNotNull(XqWorkOrderDO::getContentTitle)
                    .ne(XqWorkOrderDO::getContentTitle, "");
        }
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            List<Long> parentIds = childParentIds;
            wrapper.and(w -> {
                w.like(XqWorkOrderDO::getNo, reqVO.getKeyword())
                        .or()
                        .like(XqWorkOrderDO::getTitle, reqVO.getKeyword())
                        .or()
                        .like(XqWorkOrderDO::getExternalSku, reqVO.getKeyword());
                if (cn.hutool.core.collection.CollUtil.isNotEmpty(parentIds)) {
                    w.or().in(XqWorkOrderDO::getId, parentIds);
                }
            });
        }
        wrapper.orderByDesc(XqWorkOrderDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 已选产品池：进行中 + 已领取 + 已写图片提示词 + 未选平台。
     * 运营在此补绑平台/分类；同 SKU 只保留无平台一单，不做「有平台无分类」兼收。
     */
    private static void applySelectedPoolFilter(LambdaQueryWrapperX<XqWorkOrderDO> wrapper) {
        wrapper.eq(XqWorkOrderDO::getStatus, 10);
        wrapper.and(w -> w.isNotNull(XqWorkOrderDO::getAssigneeUserId)
                .or().isNotNull(XqWorkOrderDO::getCopyUserId)
                .or().isNotNull(XqWorkOrderDO::getImageUserId));
        wrapper.isNotNull(XqWorkOrderDO::getImagePromptJson)
                .ne(XqWorkOrderDO::getImagePromptJson, "")
                .ne(XqWorkOrderDO::getImagePromptJson, "[]");
        wrapper.and(w -> w.isNull(XqWorkOrderDO::getListingPlatformId)
                .or().eq(XqWorkOrderDO::getListingPlatformId, ""));
    }

    /**
     * 图片状态 Tab：pending 与列表展示一致，把 null/空 也算作「待执行」；
     * todo（未完成）仍单独精确匹配，不与 pending 混进同一筛选项。
     */
    private static void applyImageStatusFilter(LambdaQueryWrapperX<XqWorkOrderDO> wrapper,
                                               String imageStatus) {
        if (imageStatus == null || imageStatus.isBlank()) {
            return;
        }
        String s = imageStatus.trim().toLowerCase();
        if ("pending".equals(s)) {
            wrapper.and(w -> w.isNull(XqWorkOrderDO::getImageStatus)
                    .or()
                    .eq(XqWorkOrderDO::getImageStatus, "")
                    .or()
                    .eq(XqWorkOrderDO::getImageStatus, "pending"));
            return;
        }
        wrapper.eq(XqWorkOrderDO::getImageStatus, s);
    }

    private static void applyFlowStatus(LambdaQueryWrapperX<XqWorkOrderDO> wrapper, String flowStatus) {
        if (flowStatus == null || flowStatus.isBlank()) {
            return;
        }
        switch (flowStatus) {
            case "listed", "submitted" -> wrapper.eq(XqWorkOrderDO::getStatus, 20);
            case "closed" -> wrapper.eq(XqWorkOrderDO::getStatus, 30);
            case "copy_pending" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .and(w -> w.isNull(XqWorkOrderDO::getContentTitle)
                            .or()
                            .eq(XqWorkOrderDO::getContentTitle, ""))
                    .and(w -> w.isNull(XqWorkOrderDO::getRpaCopyStatus)
                            .or()
                            .notIn(XqWorkOrderDO::getRpaCopyStatus, "running", "queued"));
            case "copy_running", "writing" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .in(XqWorkOrderDO::getRpaCopyStatus, "running", "queued");
            case "assign_image", "review" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .isNotNull(XqWorkOrderDO::getContentTitle)
                    .ne(XqWorkOrderDO::getContentTitle, "")
                    .isNull(XqWorkOrderDO::getImageUserId);
            case "image_pending" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .isNotNull(XqWorkOrderDO::getImageUserId)
                    .and(w -> w.isNull(XqWorkOrderDO::getImageStatus)
                            .or()
                            .in(XqWorkOrderDO::getImageStatus, "todo", "pending"));
            case "image_running" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .isNotNull(XqWorkOrderDO::getImageUserId)
                    .in(XqWorkOrderDO::getImageStatus, "running", "rejected");
            // 已出图待运营确认图文
            case "review_pending" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .isNotNull(XqWorkOrderDO::getImageUserId)
                    .in(XqWorkOrderDO::getImageStatus, "generated", "revised");
            // 审核通过（done）后才进待上架
            case "list_pending" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .and(w -> w.eq(XqWorkOrderDO::getImageStatus, "done")
                            .or()
                            .eq(XqWorkOrderDO::getListingStatus, "export_pending_confirm")
                            .or()
                            .eq(XqWorkOrderDO::getWorkflowPhase, "list"));
            case "assign" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .isNull(XqWorkOrderDO::getCopyUserId)
                    .and(w -> w.isNull(XqWorkOrderDO::getContentTitle)
                            .or()
                            .eq(XqWorkOrderDO::getContentTitle, ""))
                    .and(w -> w.isNull(XqWorkOrderDO::getRpaCopyStatus)
                            .or()
                            .notIn(XqWorkOrderDO::getRpaCopyStatus, "running", "queued"));
            default -> {
            }
        }
    }

    @Select("SELECT DISTINCT parent_work_order_id FROM xq_work_order "
            + "WHERE deleted = 0 AND parent_work_order_id IS NOT NULL "
            + "AND (external_sku LIKE CONCAT('%', #{kw}, '%') "
            + "OR title LIKE CONCAT('%', #{kw}, '%') OR no LIKE CONCAT('%', #{kw}, '%'))")
    List<Long> selectParentIdsByChildKeyword(@Param("kw") String kw);

    @Select("SELECT DISTINCT parent_work_order_id FROM xq_work_order "
            + "WHERE deleted = 0 AND parent_work_order_id IS NOT NULL "
            + "AND image_user_id = #{uid}")
    List<Long> selectParentIdsByImageUserId(@Param("uid") Long uid);

    @Select("SELECT id FROM xq_work_order "
            + "WHERE deleted = 0 AND parent_work_order_id IS NULL "
            + "AND image_user_id = #{uid}")
    List<Long> selectRootIdsByImageUserId(@Param("uid") Long uid);

    /**
     * 图片池：凡家族内任一 SKU 分给该美工，则主体+全部变体一并进入扁平列表。
     */
    default void applyImagePoolFamilyFilter(LambdaQueryWrapperX<XqWorkOrderDO> wrapper, Long artistId) {
        if (artistId == null) {
            wrapper.eq(XqWorkOrderDO::getImageUserId, -1L);
            return;
        }
        LinkedHashSet<Long> rootIds = new LinkedHashSet<>();
        List<Long> roots = selectRootIdsByImageUserId(artistId);
        if (roots != null) {
            rootIds.addAll(roots);
        }
        List<Long> parentsOfChildren = selectParentIdsByImageUserId(artistId);
        if (parentsOfChildren != null) {
            for (Long pid : parentsOfChildren) {
                if (pid != null) {
                    rootIds.add(pid);
                }
            }
        }
        if (rootIds.isEmpty()) {
            // 无家族根：仍返回直接分给该美工的行（含孤立变体）
            wrapper.eq(XqWorkOrderDO::getImageUserId, artistId);
            return;
        }
        wrapper.and(w -> w.eq(XqWorkOrderDO::getImageUserId, artistId)
                .or()
                .in(XqWorkOrderDO::getId, rootIds)
                .or()
                .in(XqWorkOrderDO::getParentWorkOrderId, rootIds));
    }

    default List<XqWorkOrderDO> selectByParentIds(Collection<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .in(XqWorkOrderDO::getParentWorkOrderId, parentIds)
                .orderByAsc(XqWorkOrderDO::getId));
    }

    /** 当前用户工作台上进行中的任务（下发认领人 = 我），含变体，不按文案状态筛。 */
    default List<XqWorkOrderDO> selectDoingByAssignee(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, 10)
                .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                .orderByAsc(XqWorkOrderDO::getId)
                .last("LIMIT " + n));
    }

    /**
     * 当前登录人工作台上的待跑文案（下发认领人 = 我）。
     * queued / running 在内存筛，避免 MP 嵌套 OR 把 update_time 套到刚入队的 queued 上。
     */
    default List<XqWorkOrderDO> selectPendingCopyJobs(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 20));
        java.time.LocalDateTime stuckBefore = java.time.LocalDateTime.now().minusMinutes(1);
        List<XqWorkOrderDO> rows = selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, 10)
                .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                .in(XqWorkOrderDO::getRpaCopyStatus, "queued", "running")
                .orderByAsc(XqWorkOrderDO::getId)
                .last("LIMIT " + Math.max(n * 3, 30)));
        List<XqWorkOrderDO> out = new java.util.ArrayList<>();
        for (XqWorkOrderDO row : rows) {
            String st = row.getRpaCopyStatus();
            if ("queued".equalsIgnoreCase(st == null ? "" : st.trim())) {
                out.add(row);
            } else if ("running".equalsIgnoreCase(st == null ? "" : st.trim())
                    && row.getUpdateTime() != null
                    && row.getUpdateTime().isBefore(stuckBefore)) {
                out.add(row);
            }
            if (out.size() >= n) {
                break;
            }
        }
        return out;
    }

    /**
     * 按 SKU 取文案详情工单。优先 running（刚被 pull 标记），再 queued，最后才按 id 最大。
     * 避免同 SKU 多单时 detail?sku= 命中新单、pull 却标记了旧单，回调与界面分裂。
     */
    default XqWorkOrderDO selectDoingCopyBySku(Long userId, String sku) {
        XqWorkOrderDO running = selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, sku)
                .eq(XqWorkOrderDO::getStatus, 10)
                .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                .eq(XqWorkOrderDO::getRpaCopyStatus, "running")
                .orderByDesc(XqWorkOrderDO::getId)
                .last("LIMIT 1"));
        if (running != null) {
            return running;
        }
        XqWorkOrderDO queued = selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, sku)
                .eq(XqWorkOrderDO::getStatus, 10)
                .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                .eq(XqWorkOrderDO::getRpaCopyStatus, "queued")
                .orderByDesc(XqWorkOrderDO::getId)
                .last("LIMIT 1"));
        if (queued != null) {
            return queued;
        }
        return selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, sku)
                .eq(XqWorkOrderDO::getStatus, 10)
                .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                .orderByDesc(XqWorkOrderDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 待跑生图：运营（copyUserId/assignee）本机 RPA 优先领取；
     * 也允许美工（imageUserId）自拉。queued；以及超过 2 分钟仍 stuck 在 running 的回收。
     */
    default List<XqWorkOrderDO> selectPendingImageJobs(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 20));
        java.time.LocalDateTime stuckBefore = java.time.LocalDateTime.now().minusMinutes(2);
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, 10)
                .and(w -> w.eq(XqWorkOrderDO::getCopyUserId, userId)
                        .or()
                        .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                        .or()
                        .eq(XqWorkOrderDO::getImageUserId, userId))
                .and(w -> w.eq(XqWorkOrderDO::getRpaImageStatus, "queued")
                        .or(w2 -> w2.eq(XqWorkOrderDO::getRpaImageStatus, "running")
                                .lt(XqWorkOrderDO::getUpdateTime, stuckBefore)))
                .orderByAsc(XqWorkOrderDO::getId)
                .last("LIMIT " + n));
    }

    /** 进行中的同 SKU 任务 */
    default XqWorkOrderDO selectDoingByExternalSku(String externalSku) {
        return selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, externalSku)
                .eq(XqWorkOrderDO::getStatus, 10)
                .last("LIMIT 1"));
    }

    /**
     * 同 SKU + 同平台：进行中(10) 或已上架(20) 的任务（关闭后可再下发）
     */
    default XqWorkOrderDO selectActiveBySkuAndPlatform(String externalSku, String listingPlatformId) {
        return selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, externalSku)
                .eq(XqWorkOrderDO::getListingPlatformId, listingPlatformId)
                .in(XqWorkOrderDO::getStatus, 10, 20)
                .last("LIMIT 1"));
    }

    /** 列表展示：SKU → 已选取平台 */
    default List<XqWorkOrderDO> selectPickedMarksBySkus(Collection<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .select(XqWorkOrderDO::getExternalSku,
                        XqWorkOrderDO::getListingPlatformId,
                        XqWorkOrderDO::getListingPlatformName)
                .in(XqWorkOrderDO::getExternalSku, skus)
                .in(XqWorkOrderDO::getStatus, 10, 20)
                .isNotNull(XqWorkOrderDO::getListingPlatformId)
                .ne(XqWorkOrderDO::getListingPlatformId, ""));
    }

    /** 筛出本平台已选：该平台进行中/已上架的 SKU */
    default List<String> selectActiveSkusByPlatform(String listingPlatformId) {
        if (listingPlatformId == null || listingPlatformId.isBlank()) {
            return List.of();
        }
        List<XqWorkOrderDO> rows = selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .select(XqWorkOrderDO::getExternalSku)
                .eq(XqWorkOrderDO::getListingPlatformId, listingPlatformId)
                .in(XqWorkOrderDO::getStatus, 10, 20)
                .isNotNull(XqWorkOrderDO::getExternalSku)
                .ne(XqWorkOrderDO::getExternalSku, ""));
        LinkedHashSet<String> skus = new LinkedHashSet<>();
        for (XqWorkOrderDO row : rows) {
            if (row.getExternalSku() != null && !row.getExternalSku().isBlank()) {
                skus.add(row.getExternalSku().trim());
            }
        }
        return List.copyOf(skus);
    }

    /**
     * 下发前反查：同 SKU 在其它平台已有文案或图片的任务
     */
    default List<XqWorkOrderDO> selectReusableBySkus(Collection<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .in(XqWorkOrderDO::getExternalSku, skus)
                .in(XqWorkOrderDO::getStatus, 10, 20)
                .and(w -> w
                        .and(c -> c.isNotNull(XqWorkOrderDO::getContentTitle)
                                .ne(XqWorkOrderDO::getContentTitle, ""))
                        .or(i -> i.isNotNull(XqWorkOrderDO::getGeneratedImageUrl)
                                .ne(XqWorkOrderDO::getGeneratedImageUrl, ""))
                        .or(p -> p.isNotNull(XqWorkOrderDO::getImagePromptJson)
                                .ne(XqWorkOrderDO::getImagePromptJson, "")
                                .ne(XqWorkOrderDO::getImagePromptJson, "[]")))
                .orderByDesc(XqWorkOrderDO::getId));
    }

    /**
     * 关闭并清理进度：文案/图片/领取人/美工一并清空，避免仍出现在「我的文案」等池子
     */
    default void closeAndClearProgress(Long id) {
        update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, id)
                .eq(XqWorkOrderDO::getStatus, 10)
                .set(XqWorkOrderDO::getStatus, 30)
                .set(XqWorkOrderDO::getWorkflowPhase, "closed")
                .set(XqWorkOrderDO::getContentTitle, null)
                .set(XqWorkOrderDO::getContentSellingPoints, null)
                .set(XqWorkOrderDO::getCopyResultJson, null)
                .set(XqWorkOrderDO::getImagePromptJson, null)
                .set(XqWorkOrderDO::getGeneratedImageUrl, null)
                .set(XqWorkOrderDO::getCopyUserId, null)
                .set(XqWorkOrderDO::getImageUserId, null)
                .set(XqWorkOrderDO::getImageStatus, null)
                .set(XqWorkOrderDO::getAssigneeUserId, null)
                .set(XqWorkOrderDO::getProductId, null)
                .set(XqWorkOrderDO::getProductSku, null)
                .set(XqWorkOrderDO::getRpaCopyWorkUuid, null)
                .set(XqWorkOrderDO::getRpaCopyStatus, null)
                .set(XqWorkOrderDO::getRpaCopyError, null));
    }

}
