package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "保存平台文案规则")
@Data
public class XqCopyGenRuleSaveReqVO {

    @Schema(description = "平台ID，空字符串=通用规则")
    private String platformId;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则名称不能为空")
    private String name;

    @Schema(description = "规则 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "configJson 不能为空")
    private String configJson;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;
}
