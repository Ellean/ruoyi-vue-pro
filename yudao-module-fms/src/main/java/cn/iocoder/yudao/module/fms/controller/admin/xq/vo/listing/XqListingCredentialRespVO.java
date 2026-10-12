package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "上架平台密钥（敏感字段仅掩码）")
@Data
public class XqListingCredentialRespVO {
    private Long id;
    private Long platformId;
    private String sourcePlatformId;
    private String platformCode;
    private String platformName;
    private String provider;
    private String baseUrl;
    private String shopId;
    private String apiKeyMask;
    private String clientId;
    private String clientSecretMask;
    private String supplierId;
    private String env;
    private String extraJson;
    private Boolean enabled;
    private LocalDateTime syncedFromXqAt;
    private String remark;
    private LocalDateTime updateTime;
}
