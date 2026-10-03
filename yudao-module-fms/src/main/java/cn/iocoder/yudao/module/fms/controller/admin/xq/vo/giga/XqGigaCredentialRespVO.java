package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class XqGigaCredentialRespVO {
    private Long id;
    private String name;
    private String vendorCode;
    private String vendorName;
    private String clientId;
    private String clientSecretMask;
    private Boolean sandbox;
    private String baseUrl;
    /** pickup / dropship */
    private String priceRole;
    private Boolean enableScheduledSync;
    /** skip_if_exists / always_refresh */
    private String syncDedupeMode;
    private Boolean isDefault;
    private Boolean enabled;
    private String remark;
    private LocalDateTime updateTime;
}
