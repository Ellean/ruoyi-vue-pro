package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "AI 用量计费看板（ToAPIs）")
@Data
public class XqAiUsageDashboardRespVO {

    @Schema(description = "是否已配置账号访问令牌")
    private Boolean configured;

    @Schema(description = "提示文案")
    private String hint;

    @Schema(description = "查询窗口起点 Unix 秒")
    private Long startTimestamp;

    @Schema(description = "查询窗口终点 Unix 秒")
    private Long endTimestamp;

    @Schema(description = "窗口内总费用 USD")
    private String totalCostUsd;

    @Schema(description = "窗口内总 Token")
    private Long totalTokens;

    @Schema(description = "窗口内请求数")
    private Long totalRequests;

    @Schema(description = "按业务分类汇总：文案 / 生图 / 改图 / 其它")
    private List<CategoryBucket> categories = new ArrayList<>();

    @Schema(description = "按 API Key 汇总")
    private List<KeySummary> keys = new ArrayList<>();

    @Schema(description = "各 ToAPIs Key 余额（sk- 查询 /v1/balance）")
    private List<KeyBalance> balances = new ArrayList<>();

    @Schema(description = "最近计费明细（最多 100 条）")
    private List<UsageRow> recent = new ArrayList<>();

    @Data
    public static class CategoryBucket {
        /** copy / image_gen / image_revise / other */
        private String code;
        private String label;
        private Long requestCount;
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
        private String costUsd;
    }

    @Data
    public static class KeySummary {
        private Long tokenId;
        private String tokenName;
        private Long requestCount;
        private Long totalTokens;
        private String costUsd;
        /** 推断业务：copy / image_gen / image_revise / mixed / other */
        private String categoryHint;
    }

    @Data
    public static class KeyBalance {
        private Long apiKeyDbId;
        private String name;
        private String remainBalance;
        private String usedBalance;
        private String remainCredits;
        private String usedCredits;
        private Boolean unlimitedQuota;
        private String error;
    }

    @Data
    public static class UsageRow {
        private Long logId;
        private Long createdAt;
        private Long tokenId;
        private String tokenName;
        private String modelName;
        private String taskId;
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
        private String costUsd;
        private String category;
        private String categoryLabel;
    }
}
