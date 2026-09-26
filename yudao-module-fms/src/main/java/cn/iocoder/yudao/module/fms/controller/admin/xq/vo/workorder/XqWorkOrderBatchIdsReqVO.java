package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "批量生成文案")
@Data
public class XqWorkOrderBatchIdsReqVO {

    @NotEmpty(message = "请选择任务")
    private List<Long> ids;
}
