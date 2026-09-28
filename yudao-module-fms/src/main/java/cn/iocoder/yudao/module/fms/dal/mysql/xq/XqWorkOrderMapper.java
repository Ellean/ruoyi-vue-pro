package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import org.apache.ibatis.annotations.Mapper;

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

    /** 进行中的同 SKU 任务 */
    default XqWorkOrderDO selectDoingByExternalSku(String externalSku) {
        return selectOne(new LambdaQueryWrapperX<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getExternalSku, externalSku)
                .eq(XqWorkOrderDO::getStatus, 10)
                .last("LIMIT 1"));
    }

}
