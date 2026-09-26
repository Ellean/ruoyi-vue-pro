package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 品库产品新增/修改 Request VO")
@Data
public class XqProductSaveReqVO {

    private Long id;

    @NotEmpty(message = "SKU 不能为空")
    private String sku;

    private String itemCode;

    @NotEmpty(message = "名称不能为空")
    private String name;

    private String categoryName;
    private Long gigaCategoryId;
    private String imageUrl;
    private Integer qtyAvailable;
    private String supplierCode;
    private String supplierName;
    private String listedTag;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;

}
