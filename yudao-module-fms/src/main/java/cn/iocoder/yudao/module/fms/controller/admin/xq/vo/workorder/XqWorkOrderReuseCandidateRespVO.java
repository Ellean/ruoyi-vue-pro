package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 可复用文案/图片候选")
@Data
public class XqWorkOrderReuseCandidateRespVO {

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "候选任务列表")
    private List<Candidate> candidates = new ArrayList<>();

    @Data
    public static class Candidate {
        private Long workOrderId;
        private String no;
        private String listingPlatformId;
        private String listingPlatformName;
        private String listingShopName;
        private Integer status;
        private String workflowPhase;
        private String imageStatus;
        /** 是否有可复用文案 */
        private Boolean hasCopy;
        /** 是否有可复用图片 */
        private Boolean hasImage;
        private String contentTitle;
        private String contentSellingPoints;
        private String description;
        private String coverUrl;
        private List<String> imageUrls = new ArrayList<>();
    }

}
