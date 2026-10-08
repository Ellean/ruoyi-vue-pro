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
import java.util.List;

@Mapper
public interface XqWorkOrderMapper extends BaseMapperX<XqWorkOrderDO> {

    default PageResult<XqWorkOrderDO> selectPage(XqWorkOrderPageReqVO reqVO) {
        List<Long> childParentIds = Collections.emptyList();
        if (reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank()) {
            childParentIds = selectParentIdsByChildKeyword(reqVO.getKeyword().trim());
        }
        LambdaQueryWrapperX<XqWorkOrderDO> wrapper = new LambdaQueryWrapperX<XqWorkOrderDO>()
                .likeIfPresent(XqWorkOrderDO::getNo, reqVO.getNo())
                .likeIfPresent(XqWorkOrderDO::getTitle, reqVO.getTitle())
                .likeIfPresent(XqWorkOrderDO::getExternalSku, reqVO.getExternalSku())
                .eqIfPresent(XqWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(XqWorkOrderDO::getGigaCategoryId, reqVO.getGigaCategoryId())
                .eqIfPresent(XqWorkOrderDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                .eqIfPresent(XqWorkOrderDO::getCopyUserId, reqVO.getCopyUserId())
                .eqIfPresent(XqWorkOrderDO::getImageUserId, reqVO.getImageUserId())
                .eqIfPresent(XqWorkOrderDO::getImageStatus, reqVO.getImageStatus())
                .eqIfPresent(XqWorkOrderDO::getWorkflowPhase, reqVO.getWorkflowPhase())
                .eqIfPresent(XqWorkOrderDO::getListingPlatformId, reqVO.getListingPlatformId())
                .eqIfPresent(XqWorkOrderDO::getListingShopId, reqVO.getListingShopId())
                .eqIfPresent(XqWorkOrderDO::getListingCategoryId, reqVO.getListingCategoryId());
        applyFlowStatus(wrapper, reqVO.getFlowStatus());
        if (reqVO.getBoundUserId() != null) {
            Long uid = reqVO.getBoundUserId();
            String creator = String.valueOf(uid);
            wrapper.and(w -> w.eq(XqWorkOrderDO::getAssigneeUserId, uid)
                    .or().eq(XqWorkOrderDO::getCopyUserId, uid)
                    .or().eq(XqWorkOrderDO::getImageUserId, uid)
                    .or().eq(XqWorkOrderDO::getCreator, creator));
        }
        if (reqVO.getMineUserId() != null) {
            wrapper.and(w -> w.eq(XqWorkOrderDO::getCopyUserId, reqVO.getMineUserId())
                    .or()
                    .eq(XqWorkOrderDO::getAssigneeUserId, reqVO.getMineUserId()));
        }
        if (reqVO.getMineImageUserId() != null) {
            Long uid = reqVO.getMineImageUserId();
            // 父单自身分给美工，或任一变体分给美工，都要能在图片池看到该家族
            List<Long> parentIdsByChildImage = selectParentIdsByImageUserId(uid);
            wrapper.and(w -> {
                w.eq(XqWorkOrderDO::getImageUserId, uid)
                        .or()
                        .eq(XqWorkOrderDO::getCopyUserId, uid)
                        .or()
                        .eq(XqWorkOrderDO::getAssigneeUserId, uid);
                if (cn.hutool.core.collection.CollUtil.isNotEmpty(parentIdsByChildImage)) {
                    w.or().in(XqWorkOrderDO::getId, parentIdsByChildImage);
                }
            });
        }
        if (Boolean.TRUE.equals(reqVO.getListingReady())) {
            wrapper.in(XqWorkOrderDO::getImageStatus, "done", "generated", "revised");
        }
        wrapper.isNull(XqWorkOrderDO::getParentWorkOrderId);
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
                    .in(XqWorkOrderDO::getImageStatus, "running", "generated", "revised", "rejected");
            case "list_pending" -> wrapper.eq(XqWorkOrderDO::getStatus, 10)
                    .and(w -> w.in(XqWorkOrderDO::getImageStatus, "done", "generated", "revised")
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

    default List<XqWorkOrderDO> selectByParentIds(Collection<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .in(XqWorkOrderDO::getParentWorkOrderId, parentIds)
                .orderByAsc(XqWorkOrderDO::getId));
    }

    /**
     * 当前登录人待跑文案：扁平清单（主体/变体无主次）。
     * 含 queued；以及超过 1 分钟仍 stuck 在 running 的（识图/中途失败未回写时回收）。
     */
    default List<XqWorkOrderDO> selectPendingCopyJobs(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 20));
        java.time.LocalDateTime stuckBefore = java.time.LocalDateTime.now().minusMinutes(1);
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, 10)
                .eq(XqWorkOrderDO::getCopyUserId, userId)
                .and(w -> w.eq(XqWorkOrderDO::getRpaCopyStatus, "queued")
                        .or(w2 -> w2.eq(XqWorkOrderDO::getRpaCopyStatus, "running")
                                .lt(XqWorkOrderDO::getUpdateTime, stuckBefore)))
                .orderByAsc(XqWorkOrderDO::getId)
                .last("LIMIT " + n));
    }

    default XqWorkOrderDO selectDoingCopyBySku(Long userId, String sku) {
        return selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, sku)
                .eq(XqWorkOrderDO::getStatus, 10)
                .and(w -> w.eq(XqWorkOrderDO::getCopyUserId, userId)
                        .or()
                        .isNull(XqWorkOrderDO::getCopyUserId))
                .orderByDesc(XqWorkOrderDO::getId)
                .last("LIMIT 1"));
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
