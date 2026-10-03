package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "个人 RPA 配置 Response")
@Data
public class XqRpaConfigRespVO {

    @Schema(description = "管理员是否已配置控制中枢全局密钥")
    private Boolean globalConfigured;

    @Schema(description = "全局 API 地址（只读展示）")
    private String globalBaseUrl;

    @Schema(description = "全局 appKey 掩码（只读）")
    private String globalAppKeyMasked;

    private String imageJobUuid;
    private String copyJobUuid;
    private String erpSiteUrl;
    private String account;
    private Boolean hasPassword;

}
