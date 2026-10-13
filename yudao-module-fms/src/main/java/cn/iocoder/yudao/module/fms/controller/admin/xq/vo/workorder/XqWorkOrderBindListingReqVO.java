package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "已选产品池：为无平台/分类任务绑定上架平台与分类")
@Data
public class XqWorkOrderBindListingReqVO {

    @Schema(description = "任务 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择任务")
    private List<Long> ids;

    @Schema(description = "上架平台 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择平台")
    private String listingPlatformId;

    @Schema(description = "上架平台名")
    private String listingPlatformName;

    @Schema(description = "上架店铺 ID")
    private String listingShopId;

    @Schema(description = "上架店铺名")
    private String listingShopName;

    @Schema(description = "上架分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请选择分类")
    private String listingCategoryId;

    @Schema(description = "上架分类名")
    private String listingCategoryName;

    @Schema(description = "是否同步整家族变体，默认 true")
    private Boolean expandFamily;

}
