package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "店铺发货仓绑定")
@Data
public class XqStoreWarehouseRespVO {
    private Long id;
    private Long storeId;
    private String storeName;
    private String platformCode;
    private String platformName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String physicalCode;
    private String physicalName;
    private String countryCode;
    private Integer priority;
    private String shippingMethod;
    private String courierAccount;
    private Boolean enabled;
    private String remark;
}
