package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 原库 SKU 拼装规则 t_giga_platform_sku_rule（数据源 xq） */
@TableName("t_giga_platform_sku_rule")
@Data
@TenantIgnore
public class XqPlatformSkuRuleDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String platformId;
    private String shopId;
    /** 运营；空串 = 店铺通用 */
    private String ownerUserId;
    private String name;
    private String segmentsJson;
    private Boolean enabled;
    private Boolean skipSku;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
