package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqPlatformAliasDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqPlatformAliasMapper extends BaseMapperX<XqPlatformAliasDO> {

    default List<XqPlatformAliasDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<XqPlatformAliasDO>()
                .orderByAsc(XqPlatformAliasDO::getPlatformId)
                .orderByAsc(XqPlatformAliasDO::getAlias));
    }

    default XqPlatformAliasDO selectByAlias(String alias) {
        return selectOne(new LambdaQueryWrapperX<XqPlatformAliasDO>()
                .eq(XqPlatformAliasDO::getAlias, alias)
                .last("LIMIT 1"));
    }

    default List<XqPlatformAliasDO> selectByPlatformId(Long platformId) {
        return selectList(new LambdaQueryWrapperX<XqPlatformAliasDO>()
                .eq(XqPlatformAliasDO::getPlatformId, platformId));
    }

}
