package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "机器翻译 - 单段结果")
@Data
public class XqTranslateTextRespVO {

    @Schema(description = "译文")
    private String translated;

    @Schema(description = "检测到的源语言（SourceLanguage=auto 时）")
    private String detectedLanguage;

    @Schema(description = "使用的场景")
    private String scene;

    @Schema(description = "原文词数/字符统计（阿里云返回）")
    private String wordCount;
}
