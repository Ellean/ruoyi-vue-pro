package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 品库产品 Response VO")
@Data
public class XqProductRespVO {

    private String id;
    private String sku;
    private String itemCode;
    private String name;
    private String categoryName;
    private Long gigaCategoryId;
    private String imageUrl;
    private List<String> imageUrls = new ArrayList<>();
    private Integer imageCount;
    /** Giga 原始 HTML 文案 */
    private String description;
    private Integer qtyAvailable;
    private String supplierCode;
    private String supplierName;
    private String listedTag;
    /** 原价 */
    private BigDecimal price;
    /** 专享价 */
    private BigDecimal exclusivePrice;
    /** 折扣价 */
    private BigDecimal discountedPrice;
    private String currency;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private String mainColor;
    private List<Variant> variants = new ArrayList<>();

    @Data
    public static class Variant {
        private String id;
        private String sku;
        private String itemCode;
        private String name;
        private String imageUrl;
        private String mainColor;
        private Integer qtyAvailable;
        private BigDecimal price;
        private BigDecimal discountedPrice;
    }

}
