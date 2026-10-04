package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 原库仓库地址/物理仓 t_erp_warehouse_address（数据源 xq）
 */
@TableName("t_erp_warehouse_address")
@Data
@TenantIgnore
public class XqErpWarehouseAddressDO {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String wpCode;
    private String wpName;
    private Integer warehouseId;
    private String countryCode;
    private String countryName;
    private String status;
    private String remark;

}
