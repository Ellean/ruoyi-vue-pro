package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqUserStoreDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqUserStoreMapper extends BaseMapperX<XqUserStoreDO> {

    default List<XqUserStoreDO> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<XqUserStoreDO>()
                .eq(XqUserStoreDO::getUserId, userId)
                .orderByAsc(XqUserStoreDO::getStoreId));
    }

    default void deleteByUserId(Long userId) {
        delete(new LambdaQueryWrapperX<XqUserStoreDO>()
                .eq(XqUserStoreDO::getUserId, userId));
    }

    default long countByStoreId(Long storeId) {
        return selectCount(new LambdaQueryWrapperX<XqUserStoreDO>()
                .eq(XqUserStoreDO::getStoreId, storeId));
    }

}
