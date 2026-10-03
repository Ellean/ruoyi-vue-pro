package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "RPA 文案流水线回调")
@Data
public class XqWorkOrderRpaCopyCallbackReqVO {

    @NotNull
    private Long workOrderId;
    /** 与触发时下发的 callbackToken 一致 */
    private String callbackToken;
    private String workUuid;
    /** success / fail */
    private String status;
    private String errorMessage;

    /** 标题 */
    private String title;
    /** 卖点多行或数组 */
    private Object sellingPoints;
    /** 长描述 */
    private String description;
    /** 完整结构化文案 */
    private Map<String, Object> copyResult;
    /**
     * 图片提示词条目：
     * index / imageType / imageUrl / promptText / bindIndexes / marker
     */
    private List<Map<String, Object>> imagePrompts;

}
