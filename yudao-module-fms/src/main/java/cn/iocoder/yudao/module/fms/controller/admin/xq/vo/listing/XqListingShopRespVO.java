package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "上架店铺")
@Data
public class XqListingShopRespVO {
    private String id;
    private String platformId;
    private String code;
    private String name;
    private Boolean enabled;
    /** 后台店铺管理原始平台字段（sys_store.platform） */
    private String storePlatform;
}
