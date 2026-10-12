package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "工作台：单图 AI 改图预览（不写回，确认后再覆盖）")
@Data
public class XqWorkOrderReviseImageReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务编号不能为空")
    private Long id;

    @Schema(description = "图片槽位 index（0-based）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "槽位不能为空")
    private Integer promptIndex;

    @Schema(description = "调整说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "调整说明不能为空")
    private String revisePrompt;

    @Schema(description = "绑定参考图 URL（Giga 原图 / 其它 AI 图）")
    private List<String> referenceUrls;

    @Schema(description = "是否附带当前 AI 图作排版底稿")
    private Boolean includeCurrentAi;

    @Schema(description = "是否锁产品外形（默认 true）")
    private Boolean lockProduct;

}
