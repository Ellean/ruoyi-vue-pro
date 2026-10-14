package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "机器翻译 - 文案中文对照结果")
@Data
public class XqTranslateCopyPreviewRespVO {

    @Schema(description = "标题译文")
    private String titleZh;

    @Schema(description = "卖点译文（与入参下标对齐，空原文对应空串）")
    private List<String> sellingPointsZh = new ArrayList<>();

    @Schema(description = "长描述译文")
    private String descriptionZh;

    @Schema(description = "是否已配置阿里云密钥")
    private Boolean configured;
}
