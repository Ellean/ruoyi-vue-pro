package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "RPA 生图流水线回调")
@Data
public class XqWorkOrderRpaImageCallbackReqVO {

    @NotNull
    private Long workOrderId;
    /** 与触发时下发的 callbackToken 一致 */
    private String callbackToken;
    private String workUuid;
    /** success / fail */
    private String status;
    private String errorMessage;

    /**
     * 出图结果条目（覆盖写回 imagePromptJson）：
     * index / imageType / imageUrl / resultUrl / generatedImageUrl / promptText / bindIndexes / marker
     */
    private List<Map<String, Object>> imagePrompts;

    /** 首图 / 封面成品 URL（可选） */
    private String generatedImageUrl;

}
