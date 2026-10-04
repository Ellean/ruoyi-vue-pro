package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("xq_store")
@KeySequence("xq_store_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqStoreDO extends BaseDO {

    @TableId
    private Long id;
    private String name;
    private Long platformId;
    /** 原库 sys_store.id */
    private Integer sourceStoreId;
    private String account;
    private String passwordEnc;
    private String passwordMask;
    private String imageUrl;
    private Integer status;
    private String deptId;
    private String remark;

}
