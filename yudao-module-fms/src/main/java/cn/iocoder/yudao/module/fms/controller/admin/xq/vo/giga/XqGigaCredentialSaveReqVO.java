package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class XqGigaCredentialSaveReqVO {
    private Long id;
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "Client ID 不能为空")
    private String clientId;
    @Schema(description = "新建必填；更新留空表示不改")
    private String clientSecret;
    private Boolean sandbox;
    private String baseUrl;
    private Boolean isDefault;
    private Boolean enabled;
    private String remark;
}
