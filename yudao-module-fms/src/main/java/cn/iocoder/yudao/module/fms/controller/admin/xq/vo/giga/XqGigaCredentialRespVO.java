package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class XqGigaCredentialRespVO {
    private Long id;
    private String name;
    private String clientId;
    private String clientSecretMask;
    private Boolean sandbox;
    private String baseUrl;
    private Boolean isDefault;
    private Boolean enabled;
    private String remark;
    private LocalDateTime updateTime;
}
