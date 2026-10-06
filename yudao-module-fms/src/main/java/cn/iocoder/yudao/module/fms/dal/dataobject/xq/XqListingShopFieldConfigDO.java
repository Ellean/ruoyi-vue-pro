package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺+国家字段池配置（数据源 xq）
 */
@TableName("t_giga_listing_shop_field_config")
@Data
@TenantIgnore
public class XqListingShopFieldConfigDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String platformId;
    private String shopId;
    private String country;
    private String fieldsJson;
    private String partitionsJson;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
