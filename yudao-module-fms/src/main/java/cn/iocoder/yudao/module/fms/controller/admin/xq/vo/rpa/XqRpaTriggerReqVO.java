package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 触发 RPA Request")
@Data
public class XqRpaTriggerReqVO {

    /**
     * copy = 文案图文生任务；image = 生图/拉取任务
     */
    @Schema(description = "任务类型：copy / image", requiredMode = Schema.RequiredMode.REQUIRED, example = "image")
    @NotBlank(message = "任务类型不能为空")
    private String jobKind;

}
