package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("xq_warehouse")
@KeySequence("xq_warehouse_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqWarehouseDO extends BaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    private String countryCode;
    private String countryName;
    private Integer sourceWarehouseId;
    private Integer sourceAddressId;
    private Integer sort;
    private Boolean enabled;
    private String remark;

}
