package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "测试页 - 本地 AI 图结果分页")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XqAgentAiTestPageReqVO extends PageParam {

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "SKU / 品名关键字")
    private String keyword;

    @Schema(description = "仅已选平台且绑定运营+美工")
    private Boolean assignedOnly;

}
