package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库上架店铺 t_giga_listing_shop（数据源 xq）
 */
@TableName("t_giga_listing_shop")
@Data
@TenantIgnore
public class XqListingShopDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String platformId;
    private String code;
    private String name;
    private String categoryId;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
