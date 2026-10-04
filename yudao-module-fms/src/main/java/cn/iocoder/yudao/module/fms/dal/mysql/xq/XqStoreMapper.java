package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqStoreDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface XqStoreMapper extends BaseMapperX<XqStoreDO> {

    default List<XqStoreDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqStoreDO>()
                .orderByAsc(XqStoreDO::getPlatformId)
                .orderByAsc(XqStoreDO::getName)
                .orderByAsc(XqStoreDO::getId));
    }

    default List<XqStoreDO> selectByPlatformId(Long platformId) {
        return selectList(new LambdaQueryWrapperX<XqStoreDO>()
                .eqIfPresent(XqStoreDO::getPlatformId, platformId)
                .orderByAsc(XqStoreDO::getName)
                .orderByAsc(XqStoreDO::getId));
    }

    default XqStoreDO selectBySourceStoreId(Integer sourceStoreId) {
        return selectOne(new LambdaQueryWrapperX<XqStoreDO>()
                .eq(XqStoreDO::getSourceStoreId, sourceStoreId)
                .last("LIMIT 1"));
    }

    default long countByPlatformId(Long platformId) {
        return selectCount(new LambdaQueryWrapperX<XqStoreDO>()
                .eq(XqStoreDO::getPlatformId, platformId));
    }

    default List<XqStoreDO> selectEnabledByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<XqStoreDO>()
                .in(XqStoreDO::getId, ids)
                .eq(XqStoreDO::getStatus, 1));
    }

}
