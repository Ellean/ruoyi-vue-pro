package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "XQ 平台保存")
@Data
public class XqPlatformSaveReqVO {
    private Long id;
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String sourcePlatformId;
    private Integer sort;
    private Boolean enabled;
    private String remark;
}
