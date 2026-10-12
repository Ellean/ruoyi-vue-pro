package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "单图改图异步任务")
@Data
public class XqWorkOrderReviseImageTaskVO {

    @Schema(description = "任务 ID")
    private String taskId;

    @Schema(description = "pending / running / success / fail")
    private String status;

    @Schema(description = "说明")
    private String message;

    @Schema(description = "作业单 ID")
    private Long workOrderId;

    @Schema(description = "槽位")
    private Integer promptIndex;

    private XqWorkOrderReviseImageRespVO result;

}
