package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("xq_store_warehouse")
@KeySequence("xq_store_warehouse_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqStoreWarehouseDO extends BaseDO {

    @TableId
    private Long id;
    private Long storeId;
    private Long warehouseId;
    private String physicalCode;
    private String physicalName;
    private String countryCode;
    private Integer priority;
    private String shippingMethod;
    private String courierAccount;
    private Boolean enabled;
    private String remark;

}
