package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPA 拉取待跑文案")
@Data
public class XqWorkOrderRpaCopyPullReqVO {

    @Schema(description = "最多领取条数；不传默认 20（上限 20）。扁平拉取主体+勾选变体")
    private Integer limit;

}
