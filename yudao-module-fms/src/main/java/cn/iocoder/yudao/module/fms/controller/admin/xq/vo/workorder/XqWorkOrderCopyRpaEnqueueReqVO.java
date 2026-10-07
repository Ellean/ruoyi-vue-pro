package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 勾选变体入队文案 RPA")
@Data
public class XqWorkOrderCopyRpaEnqueueReqVO {

    @Schema(description = "主体任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主体任务不能为空")
    private Long rootId;

    @Schema(description = "需要独立跑文案 RPA 的任务 ID（可含主体；主体始终会入队）")
    private List<Long> selectedIds = new ArrayList<>();
}
