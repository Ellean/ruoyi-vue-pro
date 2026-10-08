package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "保存按分类图片提示词规则")
@Data
public class XqImageGenRuleSaveReqVO {

    @Schema(description = "平台ID，空=通用")
    private String platformId;

    @Schema(description = "分类ID，空=平台默认")
    private String categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则名称不能为空")
    private String name;

    @Schema(description = "规则摘要（兼容旧字段；出图槽位以 configJson.slots 为准）")
    private String promptText;

    @Schema(description = "反向提示词")
    private String negativePrompt;

    @Schema(description = "扩展 JSON")
    private String configJson;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "备注")
    private String remark;

}
