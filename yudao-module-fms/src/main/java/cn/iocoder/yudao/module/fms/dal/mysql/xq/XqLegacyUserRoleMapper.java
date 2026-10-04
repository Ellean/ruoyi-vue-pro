package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqLegacyUserRoleDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("xq")
public interface XqLegacyUserRoleMapper {

    @Select("SELECT user_id AS userId, role_id AS roleId FROM t_user_role")
    List<XqLegacyUserRoleDO> selectAll();

}
