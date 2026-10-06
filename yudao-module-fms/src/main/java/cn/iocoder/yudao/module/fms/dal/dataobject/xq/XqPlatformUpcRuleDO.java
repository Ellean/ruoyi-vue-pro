package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 原库 UPC 规则 t_giga_platform_upc_rule（数据源 xq） */
@TableName("t_giga_platform_upc_rule")
@Data
@TenantIgnore
public class XqPlatformUpcRuleDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String platformId;
    private String shopId;
    private String name;
    /** none / auto / pool */
    private String mode;
    private String flagDigit;
    private String manufacturerCode;
    private Integer productWidth;
    private Integer productStart;
    private Boolean enabled;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
