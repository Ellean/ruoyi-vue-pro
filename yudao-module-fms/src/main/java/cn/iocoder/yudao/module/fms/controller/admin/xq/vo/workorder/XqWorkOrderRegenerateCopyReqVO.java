package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 文案 AI 重新生成")
@Data
public class XqWorkOrderRegenerateCopyReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "二次修改提示词（可选）")
    private String revisionPrompt;

    @Schema(description = "修改范围：all / title / description / feature，默认 all")
    private String target;

    @Schema(description = "卖点下标（从 0 起，target=feature 时有效）")
    private Integer featureIndex;

    @Schema(description = "当前标题（可选，提交时先落库作为参考）")
    private String contentTitle;

    @Schema(description = "当前卖点文本（可选）")
    private String contentSellingPoints;

    @Schema(description = "当前长描述（可选）")
    private String contentDescription;
}
