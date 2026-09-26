package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source.XqSourceItemPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSourceItemDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XqSourceItemMapper extends BaseMapperX<XqSourceItemDO> {

    default PageResult<XqSourceItemDO> selectPage(XqSourceItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<XqSourceItemDO>()
                .likeIfPresent(XqSourceItemDO::getExternalSku, reqVO.getExternalSku())
                .likeIfPresent(XqSourceItemDO::getTitle, reqVO.getTitle())
                .eqIfPresent(XqSourceItemDO::getClaimed, reqVO.getClaimed())
                .orderByDesc(XqSourceItemDO::getId));
    }

}
