package cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - AI API 密钥新增/修改（官方或某一中转）")
@Data
public class AiApiKeySaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Aixoras-主线")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "密钥", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "密钥不能为空")
    private String apiKey;

    @Schema(description = "平台", requiredMode = Schema.RequiredMode.REQUIRED, example = "OpenAI")
    @NotEmpty(message = "平台不能为空")
    private String platform;

    @Schema(description = "API Base URL", example = "https://api.openai.com/v1")
    private String url;

    @Schema(description = "接入类型", example = "openai_official")
    private String gatewayType;

    @Schema(description = "能力：chat,vision,image_gen,image_edit", example = "chat,vision")
    private String capabilities;

    @Schema(description = "对话模型（可选）")
    private String chatModel;

    @Schema(description = "识图模型（可选）")
    private String visionModel;

    @Schema(description = "生图模型（可选）")
    private String imageModel;

    @Schema(description = "改图模型（可选）")
    private String imageEditModel;

    @Schema(description = "生图请求风格（中转用）")
    private String imageBodyStyle;

    @Schema(description = "是否异步生图")
    private Integer supportsAsync;

    @Schema(description = "是否优先 Responses")
    private Integer preferResponsesApi;

    @Schema(description = "识图 detail")
    private String visionImageDetail;

    @Schema(description = "扩展 JSON")
    private String extraConfig;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

}
