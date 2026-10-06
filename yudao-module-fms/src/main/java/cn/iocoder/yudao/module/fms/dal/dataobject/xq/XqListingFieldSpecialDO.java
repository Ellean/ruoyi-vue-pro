package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("t_giga_listing_field_special")
@Data
@TenantIgnore
public class XqListingFieldSpecialDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String platformId;
    private String fieldCode;
    private String hierarchyCodesJson;
    private String fieldJson;
    private Boolean required;
    private String requirementLevel;
    private String fieldType;
    private String label;
    private String valuesListCode;
    private String syncVersion;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
