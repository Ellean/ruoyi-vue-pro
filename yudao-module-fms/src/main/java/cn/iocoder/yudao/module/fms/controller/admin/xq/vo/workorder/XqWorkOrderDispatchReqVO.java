package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 选品库下发工作台 Request VO")
@Data
public class XqWorkOrderDispatchReqVO {

    @Schema(description = "上架平台 ID（原库 t_giga_listing_platform）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择上架平台")
    private String listingPlatformId;

    @Schema(description = "上架店铺 ID（原库 t_giga_listing_shop，可选）")
    private String listingShopId;

    @Schema(description = "上架分类 ID（可选，已不再要求）")
    private String listingCategoryId;

    @Schema(description = "上架平台名")
    private String listingPlatformName;

    @Schema(description = "上架店铺名")
    private String listingShopName;

    @Schema(description = "上架分类名")
    private String listingCategoryName;

    @Schema(description = "SKU → 复用文案来源任务 ID")
    private Map<String, Long> reuseCopyFromIds;

    @Schema(description = "SKU → 复用图片来源任务 ID")
    private Map<String, Long> reuseImageFromIds;

    @Schema(description = "选中的产品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请先选择要下发的产品")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @Schema(description = "Giga 产品 list id（用于拉取原文案/原图）")
        private String productId;

        @Schema(description = "SKU / Item Code", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "SKU 不能为空")
        private String sku;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "封面图")
        private String coverUrl;

        @Schema(description = "分类名")
        private String categoryName;

        @Schema(description = "Giga 类目 id")
        private Long gigaCategoryId;
    }

}
