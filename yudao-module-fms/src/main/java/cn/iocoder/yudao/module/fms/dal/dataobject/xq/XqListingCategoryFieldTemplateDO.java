package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库类目字段模板 t_giga_listing_category_field_template（数据源 xq）
 */
@TableName("t_giga_listing_category_field_template")
@Data
@TenantIgnore
public class XqListingCategoryFieldTemplateDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String platformId;
    private String categoryId;
    private String hierarchyCode;
    private String templateJson;
    private String syncVersion;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
