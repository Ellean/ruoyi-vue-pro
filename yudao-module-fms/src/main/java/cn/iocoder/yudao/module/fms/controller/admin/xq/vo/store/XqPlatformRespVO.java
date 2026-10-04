package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "XQ 平台")
@Data
public class XqPlatformRespVO {
    private Long id;
    private String code;
    private String name;
    private String sourcePlatformId;
    private Integer sort;
    private Boolean enabled;
    private String remark;
    private Integer storeCount;
}
