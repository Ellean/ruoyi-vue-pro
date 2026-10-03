package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 触发 RPA Response")
@Data
public class XqRpaTriggerRespVO {

    @Schema(description = "任务类型")
    private String jobKind;

    @Schema(description = "任务 UUID")
    private String jobUuid;

    @Schema(description = "运行 ID")
    private String workUuid;

    @Schema(description = "是否已下发入参")
    private Boolean inputParamSent;

}
