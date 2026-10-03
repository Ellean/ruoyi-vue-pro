package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPA 拉取待跑文案")
@Data
public class XqWorkOrderRpaCopyPullReqVO {

    @Schema(description = "最多领取条数，默认 10，最大 20")
    private Integer limit;

}
