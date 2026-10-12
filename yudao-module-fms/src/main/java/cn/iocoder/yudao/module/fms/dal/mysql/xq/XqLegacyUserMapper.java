package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqLegacyUserDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqLegacyUserMapper extends BaseMapperX<XqLegacyUserDO> {

    default List<XqLegacyUserDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<XqLegacyUserDO>()
                .and(w -> w.eq(XqLegacyUserDO::getLogicDelete, false)
                        .or().isNull(XqLegacyUserDO::getLogicDelete))
                .and(w -> w.eq(XqLegacyUserDO::getDisable, false)
                        .or().isNull(XqLegacyUserDO::getDisable)));
    }

}
