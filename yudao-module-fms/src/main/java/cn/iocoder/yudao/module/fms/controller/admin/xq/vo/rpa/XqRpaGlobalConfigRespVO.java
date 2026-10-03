package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class XqRpaGlobalConfigRespVO {
    private Boolean configured;
    private String baseUrl;
    private String appKeyMasked;
    private Boolean hasAppSecret;
    private String remark;
}
