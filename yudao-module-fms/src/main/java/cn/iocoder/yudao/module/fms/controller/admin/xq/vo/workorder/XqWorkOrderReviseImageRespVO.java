package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "工作台：单图 AI 改图预览结果")
@Data
public class XqWorkOrderReviseImageRespVO {

    @Schema(description = "预览图 URL（尚未写回任务）")
    private String previewUrl;

    @Schema(description = "当前 AI 图（回写目标，改前）")
    private String currentAiUrl;

    @Schema(description = "实际使用的参考图")
    private List<String> usedReferenceUrls;

    @Schema(description = "合并后的提示词（含锁产品前缀）")
    private String promptUsed;

    @Schema(description = "模型")
    private String model;

    @Schema(description = "耗时毫秒")
    private Long costMs;

}
