package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import lombok.Data;

import java.time.LocalDateTime;

/** 本地已同步 AI 图结果（测试页列表行） */
@Data
public class XqAgentAiResultRow {

    private String id;
    private String sku;
    private String productName;
    private Integer imageCount;
    private String coverUrl;
    private String batchNo;
    private String generatedAt;
    private LocalDateTime updateDate;
    private String listingPlatformId;
    private String platformName;
    private String shopId;
    private String listingCategoryId;
    private String categoryName;
    private String assignOwnerUserId;
    private String assignOwnerName;
    private String assignCopyUserId;
    private String assignCopyName;
    private String assignImageUserId;
    private String assignImageName;
    private String assignPhase;
    private String copyTitle;
    private Boolean hasCopy;
    private Integer bulletCount;
    /** 详情用：完整 agent_results_json */
    private String agentResultsJson;

}
