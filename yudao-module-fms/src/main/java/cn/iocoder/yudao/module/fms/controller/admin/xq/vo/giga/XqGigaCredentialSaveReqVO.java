package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class XqGigaCredentialSaveReqVO {
    private Long id;
    @NotBlank(message = "名称不能为空")
    private String name;
    @Schema(description = "商家编码，多家分组")
    private String vendorCode;
    private String vendorName;
    @NotBlank(message = "Client ID 不能为空")
    private String clientId;
    @Schema(description = "新建必填；更新留空表示不改")
    private String clientSecret;
    private Boolean sandbox;
    private String baseUrl;
    @NotBlank(message = "请标记价格角色")
    @Schema(description = "pickup=自提 / dropship=一键代发", requiredMode = Schema.RequiredMode.REQUIRED)
    private String priceRole;
    @Schema(description = "是否用于定时拉取选品库")
    private Boolean enableScheduledSync;
    @Schema(description = "skip_if_exists / always_refresh")
    private String syncDedupeMode;
    private Boolean isDefault;
    private Boolean enabled;
    private String remark;
}
