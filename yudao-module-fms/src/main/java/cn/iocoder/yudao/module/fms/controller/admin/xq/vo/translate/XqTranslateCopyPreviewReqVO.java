package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "机器翻译 - 文案中文对照（只译不写回）")
@Data
public class XqTranslateCopyPreviewReqVO {

    @Schema(description = "标题（英文）")
    private String title;

    @Schema(description = "卖点列表（英文）")
    private List<String> sellingPoints;

    @Schema(description = "长描述（英文）")
    private String description;

    @Schema(description = "源语言，默认 auto")
    private String sourceLanguage;

    @Schema(description = "目标语言，默认 zh")
    private String targetLanguage;
}
