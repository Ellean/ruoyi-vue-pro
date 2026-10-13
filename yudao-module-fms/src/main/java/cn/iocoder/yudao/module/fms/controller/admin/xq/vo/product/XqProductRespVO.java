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
    /** 自提运费 */
    private BigDecimal pickupShippingFee;
    /** 代发运费 */
    private BigDecimal dropshipShippingFee;
    private String currency;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private String mainColor;
    private String familySku;
    private String upc;
    private java.math.BigDecimal lengthCm;
    private java.math.BigDecimal widthCm;
    private java.math.BigDecimal heightCm;
    private java.math.BigDecimal lengthIn;
    private java.math.BigDecimal widthIn;
    private java.math.BigDecimal heightIn;
    private String lengthUnit;
    private java.math.BigDecimal weight;
    private java.math.BigDecimal weightKg;
    private String weightUnit;
    private java.math.BigDecimal assembledLength;
    private java.math.BigDecimal assembledWidth;
    private java.math.BigDecimal assembledHeight;
    private java.math.BigDecimal assembledWeight;
    private String assembledLengthUnit;
    private String assembledWeightUnit;
    private String mainMaterial;
    private String placeOfOrigin;
    private String brandName;
    private String characteristics;
    private String attributesJson;
    private List<Variant> variants = new ArrayList<>();
    /** 已选取过的上架平台（家族 SKU 并集） */
    private List<PickedPlatform> pickedPlatforms = new ArrayList<>();

    @Data
    public static class PickedPlatform {
        private String id;
        private String name;
    }

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
        private BigDecimal pickupShippingFee;
        private BigDecimal dropshipShippingFee;
    }

}
