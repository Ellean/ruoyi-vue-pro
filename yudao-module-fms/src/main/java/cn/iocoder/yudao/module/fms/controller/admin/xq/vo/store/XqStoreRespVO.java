package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "XQ 店铺")
@Data
public class XqStoreRespVO {
    private Long id;
    private String name;
    private Long platformId;
    private String platformCode;
    private String platformName;
    private String sourcePlatformId;
    private Integer sourceStoreId;
    private String account;
    private String passwordMask;
    private Boolean hasPassword;
    private String imageUrl;
    private Integer status;
    private String deptId;
    private String remark;
}
