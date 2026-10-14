package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "单个工单 AI 消耗汇总")
@Data
public class XqWorkOrderAiUsageVO {

    private Long workOrderId;

    @Schema(description = "文案调用次数")
    private Integer copyCalls = 0;
    @Schema(description = "文案 Tokens")
    private Long copyTokens = 0L;

    @Schema(description = "生图调用次数")
    private Integer imageGenCalls = 0;
    @Schema(description = "生图 Tokens")
    private Long imageGenTokens = 0L;

    @Schema(description = "改图调用次数")
    private Integer imageReviseCalls = 0;
    @Schema(description = "改图 Tokens")
    private Long imageReviseTokens = 0L;

    @Schema(description = "总调用次数")
    private Integer totalCalls = 0;
    @Schema(description = "总 Tokens")
    private Long totalTokens = 0L;

    @Schema(description = "预计已使用费用 USD（按 ToAPIs 公开单价估算，非账单）")
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

    @Schema(description = "费用短文案，如 约$0.12")
    private String costLabel;

    @Schema(description = "列表展示短文案，如 文案2 · 生图8 · 改图1 · 约$0.12")
    private String label;
}
