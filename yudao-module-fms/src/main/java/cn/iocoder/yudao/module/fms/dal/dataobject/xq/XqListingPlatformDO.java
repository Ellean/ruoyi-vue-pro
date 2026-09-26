package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库上架平台 t_giga_listing_platform（数据源 xq）
 */
@TableName("t_giga_listing_platform")
@Data
@TenantIgnore
public class XqListingPlatformDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String code;
    private String name;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
