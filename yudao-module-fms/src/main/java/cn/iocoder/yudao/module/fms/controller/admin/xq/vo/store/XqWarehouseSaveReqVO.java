package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "保存发货仓库")
@Data
public class XqWarehouseSaveReqVO {
    private Long id;
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String countryCode;
    private String countryName;
    private Integer sort;
    private Boolean enabled;
    private String remark;
}
