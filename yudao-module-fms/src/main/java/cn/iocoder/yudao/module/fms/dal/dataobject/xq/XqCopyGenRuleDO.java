package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 原库按平台文案规则 t_giga_copy_gen_rule（数据源 xq）
 */
@TableName("t_giga_copy_gen_rule")
@Data
@TenantIgnore
public class XqCopyGenRuleDO {

    @TableId(type = IdType.INPUT)
    private String id;
    private String platformId;
    private String code;
    private String name;
    private String configJson;
    private Boolean enabled;
    private String remark;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

}
