package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Giga 本地产品行（list ⋈ 最优 snapshot），来自 xq_finance_test
 */
@Data
public class XqGigaProductRow {

    private String id;
    private String sku;
    private String itemCode;
    private String name;
    private String categoryName;
    private Long gigaCategoryId;
    private String imageUrl;
    /** detail_json.imageUrls JSON 数组字符串 */
    private String imageUrlsJson;
    private Integer imageCount;
    /** HTML 文案 description */
    private String description;
    private Integer qtyAvailable;
    private String supplierCode;
    private String supplierName;
    /** 原价 */
    private BigDecimal price;
    /** 专享价 */
    private BigDecimal exclusivePrice;
    /** 折扣价 */
    private BigDecimal discountedPrice;
    private String currency;
    private Boolean skuAvailable;
    private String listedTag;
    private LocalDateTime createTime;
    private String remark;
    /** detail_json.mainColor */
    private String mainColor;
    /** detail_json.upc */
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
    /** detail_json.associateProductList JSON */
    private String associateProductListJson;
    /** detail_json.associateProductInfo JSON */
    private String associateProductInfoJson;
    /** t_gigab2b_product_family_index.family_sku */
    private String familySku;

}
