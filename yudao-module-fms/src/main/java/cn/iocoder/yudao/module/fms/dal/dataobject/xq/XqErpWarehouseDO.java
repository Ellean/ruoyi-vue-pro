package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 原库仓库 t_erp_warehouse（数据源 xq）
 */
@TableName("t_erp_warehouse")
@Data
@TenantIgnore
public class XqErpWarehouseDO {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseNameText;
    private String countryCode;
    private String countryName;
    private String status;
    private String remark;

}
