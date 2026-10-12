package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "上架平台")
@Data
public class XqListingPlatformRespVO {
    private String id;
    private String code;
    private String name;
    private Integer sortOrder;
    private Boolean enabled;
    /** 原库 catalog_source：excel_template / mirakl_api / wayfair_api */
    private String catalogSource;
    /** 归一：export | api */
    private String listingMode;
    /** 原库 t_giga_listing_platform.id */
    private String sourcePlatformId;
}
