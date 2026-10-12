package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 工作台 RPA 个人配置（迁移优化：只保留触发所需字段）
 */
@TableName("xq_rpa_user_config")
@KeySequence("xq_rpa_user_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqRpaUserConfigDO extends BaseDO {

    @TableId
    private Long id;
    private Long userId;
    private String baseUrl;
    private String appKey;
    private String appSecret;
    /** 生图/拉取任务 UUID */
    private String imageJobUuid;
    /** 文案图文生任务 UUID */
    private String copyJobUuid;
    private String erpSiteUrl;
    private String account;
    private String password;

    /**
     * 执行器：commander（影刀控制中枢，默认）/ script（自写脚本）/ exe（本地可执行文件）
     */
    private String runnerMode;
    /** script/exe 绝对路径 */
    private String runnerPath;
    /** 附加参数，支持 {jobKind} {workUuid} {jobUuid} */
    private String runnerArgs;
    /** 工作目录（可选） */
    private String runnerWorkdir;

}
