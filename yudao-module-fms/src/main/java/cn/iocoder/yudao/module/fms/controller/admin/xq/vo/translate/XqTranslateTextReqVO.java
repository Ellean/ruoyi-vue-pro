package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "机器翻译 - 单段文本")
@Data
public class XqTranslateTextReqVO {

    @Schema(description = "待翻译文本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "待翻译文本不能为空")
    private String text;

    @Schema(description = "源语言，默认 auto")
    private String sourceLanguage;

    @Schema(description = "目标语言，默认 zh")
    private String targetLanguage;

    @Schema(description = "场景：title / description / general，默认 description")
    private String scene;
}
