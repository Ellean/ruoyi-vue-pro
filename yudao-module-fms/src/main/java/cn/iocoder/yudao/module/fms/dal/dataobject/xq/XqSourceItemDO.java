package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 工作区-货源 DO（演示）
 */
@TableName("xq_source_item")
@KeySequence("xq_source_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqSourceItemDO extends BaseDO {

    @TableId
    private Long id;
    /** 外部 SKU */
    private String externalSku;
    /** 标题 */
    private String title;
    /** 价格 */
    private BigDecimal price;
    /** 库存 */
    private Integer stock;
    /** 来源名 */
    private String sourceName;
    /** 主图 */
    private String imageUrl;
    /** 是否已被认领 */
    private Boolean claimed;

}
