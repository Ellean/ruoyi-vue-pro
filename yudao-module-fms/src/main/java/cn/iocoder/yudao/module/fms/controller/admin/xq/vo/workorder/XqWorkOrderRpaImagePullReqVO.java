package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPA：领取待跑生图任务")
@Data
public class XqWorkOrderRpaImagePullReqVO {

    @Schema(description = "最多领取条数，默认 10，上限 20")
    private Integer limit;

}
