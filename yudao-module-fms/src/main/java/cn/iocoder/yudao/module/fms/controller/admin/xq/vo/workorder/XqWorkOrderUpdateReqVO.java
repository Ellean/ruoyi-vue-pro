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

    @Schema(description = "突出内容/长描述风格")
    private String contentHighlight;

    @Schema(description = "完整文案 JSON（可选，覆盖写）")
    private String copyResultJson;

    @Schema(description = "图片提示词 JSON（可选）")
    private String imagePromptJson;

}
