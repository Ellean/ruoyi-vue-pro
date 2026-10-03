package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XqRpaUserConfigMapper extends BaseMapperX<XqRpaUserConfigDO> {

    default XqRpaUserConfigDO selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<XqRpaUserConfigDO>()
                .eq(XqRpaUserConfigDO::getUserId, userId)
                .last("LIMIT 1"));
    }

}
