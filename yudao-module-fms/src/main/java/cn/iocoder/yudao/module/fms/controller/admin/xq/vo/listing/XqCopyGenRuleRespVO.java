package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "平台文案规则")
@Data
public class XqCopyGenRuleRespVO {
    private String id;
    private String platformId;
    private String platformCode;
    private String platformName;
    private String code;
    private String name;
    private String configJson;
    private Boolean enabled;
    private String remark;
}
