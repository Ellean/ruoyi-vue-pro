package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "XQ 平台别名保存")
@Data
public class XqPlatformAliasSaveReqVO {
    private Long id;
    @NotNull
    private Long platformId;
    @NotBlank
    private String alias;
}
