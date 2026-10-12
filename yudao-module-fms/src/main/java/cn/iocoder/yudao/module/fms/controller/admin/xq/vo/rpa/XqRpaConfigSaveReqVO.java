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

    @Schema(description = "执行器：commander / script / exe")
    private String runnerMode;
    @Schema(description = "本地脚本或 exe 路径")
    private String runnerPath;
    @Schema(description = "附加启动参数，可用 {jobKind} {workUuid} {jobUuid}")
    private String runnerArgs;
    @Schema(description = "工作目录")
    private String runnerWorkdir;

}
