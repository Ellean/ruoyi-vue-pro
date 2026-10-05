package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPA 拉取待跑文案")
@Data
public class XqWorkOrderRpaCopyPullReqVO {

    @Schema(description = "最多领取条数，默认 1，最大 5。文案只跑主体，不含变体")
    private Integer limit;

}
