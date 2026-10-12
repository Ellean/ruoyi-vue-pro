package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqLegacyRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface XqLegacyRoleMapper extends BaseMapperX<XqLegacyRoleDO> {

    default List<XqLegacyRoleDO> selectAll() {
        return selectList();
    }

}
