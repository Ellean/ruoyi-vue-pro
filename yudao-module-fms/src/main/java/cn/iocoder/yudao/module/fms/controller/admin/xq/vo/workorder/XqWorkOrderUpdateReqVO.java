package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 作业单更新 Request VO")
@Data
public class XqWorkOrderUpdateReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "文案标题")
    private String contentTitle;

    @Schema(description = "卖点（多行）")
    private String contentSellingPoints;

    @Schema(description = "突出内容风格")
    private String contentHighlight;

    @Schema(description = "英文长描述")
    private String contentDescription;

    @Schema(description = "完整文案 JSON（可选，覆盖写）")
    private String copyResultJson;

    @Schema(description = "图片提示词 JSON（可选）")
    private String imagePromptJson;

    @Schema(description = "生成主图 URL")
    private String generatedImageUrl;

    @Schema(description = "上架字段草稿 JSON")
    private String listingValuesJson;

    @Schema(description = "上架分类 ID")
    private String listingCategoryId;

    @Schema(description = "上架分类名")
    private String listingCategoryName;

    @Schema(description = "上架店铺 ID")
    private String listingShopId;

    @Schema(description = "上架店铺名")
    private String listingShopName;

}
