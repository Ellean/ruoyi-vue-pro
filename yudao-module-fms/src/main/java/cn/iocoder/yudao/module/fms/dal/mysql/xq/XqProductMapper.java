package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductPageReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqProductDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface XqProductMapper extends BaseMapperX<XqProductDO> {

    default PageResult<XqProductDO> selectPage(XqProductPageReqVO reqVO, Collection<Long> categoryIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<XqProductDO>()
                .likeIfPresent(XqProductDO::getSku, reqVO.getSku())
                .and(reqVO.getName() != null && !reqVO.getName().isEmpty(), w -> w
                        .like(XqProductDO::getName, reqVO.getName())
                        .or()
                        .like(XqProductDO::getItemCode, reqVO.getName())
                        .or()
                        .like(XqProductDO::getSku, reqVO.getName()))
                .eqIfPresent(XqProductDO::getStatus, reqVO.getStatus())
                .inIfPresent(XqProductDO::getGigaCategoryId, categoryIds)
                .orderByDesc(XqProductDO::getId));
    }

    default XqProductDO selectBySku(String sku) {
        return selectOne(XqProductDO::getSku, sku);
    }

}
