package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("t_giga_listing_field_pool")
@Data
@TenantIgnore
public class XqListingFieldPoolDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String platformId;
    private String fieldCode;
    private String label;
    private String fieldType;
    private Boolean required;
    private String requirementLevel;
    private String valuesListCode;
    private String source;
    private Integer sourceCount;
    private String fieldJson;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
