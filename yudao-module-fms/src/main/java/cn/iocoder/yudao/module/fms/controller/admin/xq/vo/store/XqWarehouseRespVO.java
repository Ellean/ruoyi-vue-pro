package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "发货仓库")
@Data
public class XqWarehouseRespVO {
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
