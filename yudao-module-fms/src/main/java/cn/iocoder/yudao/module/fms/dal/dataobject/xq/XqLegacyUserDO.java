package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 原库用户 t_user（数据源 xq）。仅用于同步绑店权限，不复制为第二套登录账号。
 */
@TableName("t_user")
@Data
@TenantIgnore
public class XqLegacyUserDO {

    @TableId
    private String id;

    /** 登录账号字段 account */
    @TableField("account")
    private String account;

    @TableField("disable")
    private Boolean disable;

    @TableField("logicDelete")
    private Boolean logicDelete;

}
