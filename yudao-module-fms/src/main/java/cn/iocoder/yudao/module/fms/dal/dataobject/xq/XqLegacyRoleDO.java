package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 原库角色 t_role（数据源 xq）
 */
@TableName("t_role")
@Data
@TenantIgnore
public class XqLegacyRoleDO {

    @TableId
    private String id;

    @TableField("name")
    private String name;

    /** 原库 type 字段 */
    @TableField("type")
    private String type;

    /** admin / leader / yyzz / operator / finance / purchase */
    private String scope;

}
