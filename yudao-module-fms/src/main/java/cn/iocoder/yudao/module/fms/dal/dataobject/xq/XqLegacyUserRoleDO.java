package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 原库用户角色 t_user_role（数据源 xq）
 */
@TableName("t_user_role")
@Data
@TenantIgnore
public class XqLegacyUserRoleDO {

    @TableField("user_id")
    private String userId;

    @TableField("role_id")
    private String roleId;

}
