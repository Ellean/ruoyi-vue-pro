package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "个人 RPA 配置保存（不含全局密钥）")
@Data
public class XqRpaConfigSaveReqVO {

    private String imageJobUuid;
    private String copyJobUuid;
    private String erpSiteUrl;
    private String account;
    @Schema(description = "密码，留空表示不修改")
    private String password;

}
