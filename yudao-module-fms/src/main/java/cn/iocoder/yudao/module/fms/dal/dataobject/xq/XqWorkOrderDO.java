package cn.iocoder.yudao.module.fms.dal.dataobject.xq;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 工作区-作业单 DO（演示）
 * <p>
 * 状态：0 待认领 / 10 进行中 / 20 已完成 / 30 已关闭
 */
@TableName("xq_work_order")
@KeySequence("xq_work_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TenantIgnore
public class XqWorkOrderDO extends BaseDO {

    @TableId
    private Long id;
    /** 作业号 */
    private String no;
    /** 货源 ID */
    private Long sourceId;
    /** 外部 SKU */
    private String externalSku;
    /** 货源标题 */
    private String title;
    /** 状态 */
    private Integer status;
    /** 文案标题 */
    private String contentTitle;
    /** 卖点 */
    private String contentSellingPoints;
    /** 入库后的品库 ID */
    private Long productId;
    /** 入库后的 SKU */
    private String productSku;
    /** 认领人 */
    private Long assigneeUserId;

}
