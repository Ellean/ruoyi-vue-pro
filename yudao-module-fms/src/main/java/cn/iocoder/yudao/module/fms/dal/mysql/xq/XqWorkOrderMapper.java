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
                .eqIfPresent(XqWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(XqWorkOrderDO::getAssigneeUserId, reqVO.getAssigneeUserId())
                .orderByDesc(XqWorkOrderDO::getId));
    }

}
