package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "更新图片作业状态")
@Data
public class XqWorkOrderImageStatusReqVO {

    @NotNull(message = "任务不能为空")
    private Long id;

    @NotBlank(message = "图片状态不能为空")
    @Schema(description = "todo / rejected / revised / done")
    private String imageStatus;

}
