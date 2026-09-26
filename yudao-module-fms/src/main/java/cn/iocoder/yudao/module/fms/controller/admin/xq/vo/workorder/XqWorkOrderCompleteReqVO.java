package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 作业完成入库 Request VO")
@Data
public class XqWorkOrderCompleteReqVO {

    @Schema(description = "作业编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "作业编号不能为空")
    private Long id;

    @Schema(description = "入库 SKU", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "SKU 不能为空")
    private String productSku;

    @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "产品名称不能为空")
    private String productName;

    @Schema(description = "分类名")
    private String categoryName;

}
