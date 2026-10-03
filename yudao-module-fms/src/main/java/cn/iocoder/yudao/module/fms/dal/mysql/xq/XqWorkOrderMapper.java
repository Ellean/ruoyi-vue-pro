package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqWorkOrderMapper extends BaseMapperX<XqWorkOrderDO> {

    default PageResult<XqWorkOrderDO> selectPage(XqWorkOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<XqWorkOrderDO>()
                .likeIfPresent(XqWorkOrderDO::getNo, reqVO.getNo())
                .likeIfPresent(XqWorkOrderDO::getTitle, reqVO.getTitle())
                .likeIfPresent(XqWorkOrderDO::getExternalSku, reqVO.getExternalSku())
                .eqIfPresent(XqWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(XqWorkOrderDO::getGigaCategoryId, reqVO.getGigaCategoryId())
                .eqIfPresent(XqWorkOrderDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                .eqIfPresent(XqWorkOrderDO::getCopyUserId, reqVO.getCopyUserId())
                .eqIfPresent(XqWorkOrderDO::getImageUserId, reqVO.getImageUserId())
                .eqIfPresent(XqWorkOrderDO::getWorkflowPhase, reqVO.getWorkflowPhase())
                .eqIfPresent(XqWorkOrderDO::getListingPlatformId, reqVO.getListingPlatformId())
                .eqIfPresent(XqWorkOrderDO::getListingShopId, reqVO.getListingShopId())
                .eqIfPresent(XqWorkOrderDO::getListingCategoryId, reqVO.getListingCategoryId())
                .and(Boolean.TRUE.equals(reqVO.getCopyReady()), w -> w

                        .isNotNull(XqWorkOrderDO::getContentTitle)
                        .ne(XqWorkOrderDO::getContentTitle, ""))
                .and(reqVO.getKeyword() != null && !reqVO.getKeyword().isBlank(), w -> w
                        .like(XqWorkOrderDO::getNo, reqVO.getKeyword())
                        .or()
                        .like(XqWorkOrderDO::getTitle, reqVO.getKeyword())
                        .or()
                        .like(XqWorkOrderDO::getExternalSku, reqVO.getKeyword()))
                .orderByDesc(XqWorkOrderDO::getId));
    }

    /** 当前登录人待跑文案：进行中、文案未完成、未在跑 */
    default List<XqWorkOrderDO> selectPendingCopyJobs(Long userId, int limit) {
        int n = Math.max(1, Math.min(limit, 20));
        return selectList(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getStatus, 10)
                .and(w -> w.isNull(XqWorkOrderDO::getContentTitle)
                        .or()
                        .eq(XqWorkOrderDO::getContentTitle, ""))
                .and(w -> w.isNull(XqWorkOrderDO::getRpaCopyStatus)
                        .or()
                        .notIn(XqWorkOrderDO::getRpaCopyStatus, "success", "running"))
                .and(w -> w.eq(XqWorkOrderDO::getCopyUserId, userId)
                        .or()
                        .isNull(XqWorkOrderDO::getCopyUserId))
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
                .set(XqWorkOrderDO::getAssigneeUserId, null)
                .set(XqWorkOrderDO::getProductId, null)
                .set(XqWorkOrderDO::getProductSku, null)
                .set(XqWorkOrderDO::getRpaCopyWorkUuid, null)
                .set(XqWorkOrderDO::getRpaCopyStatus, null)
                .set(XqWorkOrderDO::getRpaCopyError, null));
    }

}
