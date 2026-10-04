package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("xq_platform")
@KeySequence("xq_platform_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqPlatformDO extends BaseDO {

    @TableId
    private Long id;
    private String code;
    private String name;
    /** 原库 t_giga_listing_platform.id，下发/分类仍用它 */
    private String sourcePlatformId;
    private Integer sort;
    private Boolean enabled;
    private String remark;

}
