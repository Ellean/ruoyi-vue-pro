package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库上架分类 t_giga_listing_category（数据源 xq）
 */
@TableName("t_giga_listing_category")
@Data
@TenantIgnore
public class XqListingCategoryDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String name;
    private String parentId;
    private String platformId;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
