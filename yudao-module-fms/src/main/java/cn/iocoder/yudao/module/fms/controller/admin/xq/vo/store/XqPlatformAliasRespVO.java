package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "XQ 平台别名")
@Data
public class XqPlatformAliasRespVO {
    private Long id;
    private Long platformId;
    private String platformCode;
    private String platformName;
    private String alias;
}
