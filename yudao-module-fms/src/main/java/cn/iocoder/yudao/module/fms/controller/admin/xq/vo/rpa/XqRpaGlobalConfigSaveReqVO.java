package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class XqRpaGlobalConfigSaveReqVO {
    @NotBlank(message = "API 地址不能为空")
    private String baseUrl;
    @Schema(description = "留空不改")
    private String appKey;
    @Schema(description = "留空不改")
    private String appSecret;
    private String remark;
}
