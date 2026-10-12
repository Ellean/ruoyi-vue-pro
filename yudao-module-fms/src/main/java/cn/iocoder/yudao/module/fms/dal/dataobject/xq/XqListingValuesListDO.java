package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("t_giga_listing_values_list")
@Data
@TenantIgnore
public class XqListingValuesListDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String platformId;
    private String listCode;
    private String label;
    private String valuesJson;
    private String syncVersion;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
