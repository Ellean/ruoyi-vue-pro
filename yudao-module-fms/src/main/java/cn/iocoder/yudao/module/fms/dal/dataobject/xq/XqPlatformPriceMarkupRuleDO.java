package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 原库价格增幅 t_giga_platform_price_markup_rule（数据源 xq） */
@TableName("t_giga_platform_price_markup_rule")
@Data
@TenantIgnore
public class XqPlatformPriceMarkupRuleDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String platformId;
    private String shopId;
    private String ownerUserId;
    private String name;
    /** fixed / percent / ratio */
    private String mode;
    private BigDecimal value;
    private BigDecimal freightRate;
    /** dropship / pickup / both */
    private String applyTo;
    private Boolean enabled;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
