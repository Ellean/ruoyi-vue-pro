package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Schema(description = "提交上架")
@Data
public class XqWorkOrderListReqVO {

    @NotNull(message = "任务不能为空")
    private Long id;

    @Schema(description = "上架 SKU，空则用 Item Code")
    private String productSku;

    @Schema(description = "产品名称，空则用文案标题")
    private String productName;

    @Schema(description = "模板字段值 code -> value")
    private Map<String, String> values = new HashMap<>();

}
