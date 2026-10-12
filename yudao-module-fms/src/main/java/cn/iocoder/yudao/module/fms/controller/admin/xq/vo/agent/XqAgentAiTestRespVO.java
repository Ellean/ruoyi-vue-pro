package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "测试页 - 本地 AI 图结果")
@Data
public class XqAgentAiTestRespVO {

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
    /** 列表摘要：文案标题 */
    private String copyTitle;
    private Boolean hasCopy;
    private Integer bulletCount;
    private List<ImageItem> images = new ArrayList<>();
    /** 详情：完整文案 */
    private CopyItem copy;

    @Data
    public static class ImageItem {
        private String name;
        private String url;
        private String displayUrl;
        private String ossUrl;
    }

    @Data
    public static class CopyItem {
        private String title;
        private String description;
        private List<String> bullets = new ArrayList<>();
        private String productStyle;
    }

}
