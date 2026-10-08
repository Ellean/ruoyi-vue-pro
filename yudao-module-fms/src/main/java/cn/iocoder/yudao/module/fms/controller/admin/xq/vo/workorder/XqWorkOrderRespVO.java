package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 作业单 Response VO")
@Data
public class XqWorkOrderRespVO {

    private Long id;
    private String no;
    private Long sourceId;
    private String gigaProductId;
    private Long parentWorkOrderId;
    private String parentSku;
    private String variantLabel;
    private String externalSku;
    private String title;
    private String coverUrl;
    private String sourceDescription;
    private String sourceImageUrls;
    private String categoryName;
    private Long gigaCategoryId;
    private Integer status;
    private String contentTitle;
    private String contentSellingPoints;
    private String copyResultJson;
    private String imagePromptJson;
    private String generatedImageUrl;
    private Long productId;
    private String productSku;
    private Long assigneeUserId;
    private Long copyUserId;
    private Long imageUserId;
    private String imageStatus;
    /** 上架子状态：export_pending_confirm / listed */
    private String listingStatus;
    private String listingValuesJson;
    private String listingResultJson;
    private String listingPlatformId;
    private String listingShopId;
    private String listingCategoryId;
    private String listingPlatformName;
    private String listingShopName;
    private String listingCategoryName;
    private String listingCountryCode;
    private String listingCountryName;
    private String workflowPhase;
    private String rpaCopyWorkUuid;
    private String rpaCopyStatus;
    private String rpaCopyError;
    /** 是否勾选独立跑文案 RPA */
    private Boolean copyRpaSelected;
    private LocalDateTime createTime;
    /** 货源详情补全（非作业单表字段） */
    private String itemCode;
    private String mainColor;
    private String upc;
    private java.math.BigDecimal price;
    private String currency;
    private Integer qtyAvailable;
    private java.math.BigDecimal lengthCm;
    private java.math.BigDecimal widthCm;
    private java.math.BigDecimal heightCm;
    /** 变体任务（仅主体返回） */
    private List<XqWorkOrderRespVO> variants = new ArrayList<>();

}
