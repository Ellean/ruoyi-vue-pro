package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "保存店铺发货仓绑定")
@Data
public class XqStoreWarehouseSaveReqVO {
    private Long id;
    @NotNull
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
