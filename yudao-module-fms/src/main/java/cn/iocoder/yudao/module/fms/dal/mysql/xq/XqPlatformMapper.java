package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqPlatformMapper extends BaseMapperX<XqPlatformDO> {

    default List<XqPlatformDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqPlatformDO>()
                .orderByAsc(XqPlatformDO::getSort)
                .orderByAsc(XqPlatformDO::getId));
    }

    default List<XqPlatformDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<XqPlatformDO>()
                .eq(XqPlatformDO::getEnabled, true)
                .orderByAsc(XqPlatformDO::getSort)
                .orderByAsc(XqPlatformDO::getId));
    }

    default XqPlatformDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapperX<XqPlatformDO>()
                .eq(XqPlatformDO::getCode, code)
                .last("LIMIT 1"));
    }

    default XqPlatformDO selectBySourcePlatformId(String sourcePlatformId) {
        return selectOne(new LambdaQueryWrapperX<XqPlatformDO>()
                .eq(XqPlatformDO::getSourcePlatformId, sourcePlatformId)
                .last("LIMIT 1"));
    }

}
