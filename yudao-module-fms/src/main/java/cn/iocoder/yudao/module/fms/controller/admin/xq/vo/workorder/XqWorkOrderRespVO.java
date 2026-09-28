package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 作业单 Response VO")
@Data
public class XqWorkOrderRespVO {

    private Long id;
    private String no;
    private Long sourceId;
    private String externalSku;
    private String title;
    private String coverUrl;
    private String categoryName;
    private Long gigaCategoryId;
    private Integer status;
    private String contentTitle;
    private String contentSellingPoints;
    private String generatedImageUrl;
    private Long productId;
    private String productSku;
    private Long assigneeUserId;
    private Long copyUserId;
    private Long imageUserId;
    private String listingPlatformId;
    private String listingShopId;
    private String listingCategoryId;
    private String listingPlatformName;
    private String listingShopName;
    private String listingCategoryName;
    private String workflowPhase;
    private LocalDateTime createTime;

}
