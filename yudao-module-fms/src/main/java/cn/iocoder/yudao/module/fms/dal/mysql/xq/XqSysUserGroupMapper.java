package cn.iocoder.yudao.module.fms.dal.mysql.xq;

import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSysUserGroupDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DS("xq")
public interface XqSysUserGroupMapper {

    @Select("SELECT userid AS userId, groupid AS groupId FROM t_sys_user_group")
    List<XqSysUserGroupDO> selectAll();

}