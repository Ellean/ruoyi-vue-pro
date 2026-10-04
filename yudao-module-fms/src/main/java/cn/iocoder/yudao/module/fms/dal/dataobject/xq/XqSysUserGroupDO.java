package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 原库用户-店铺绑定 t_sys_user_group（数据源 xq）。
 * userid → t_user.id；groupid → sys_store.id。
 */
@TableName("t_sys_user_group")
@Data
@TenantIgnore
public class XqSysUserGroupDO {

    @TableField("userid")
    private String userId;

    /** 店铺 ID（sys_store.id） */
    @TableField("groupid")
    private Integer groupId;

}
