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
    /** 货源 ID（选品库下发可为空） */
    private Long sourceId;
    /** 外部 SKU / Item Code */
    private String externalSku;
    /** 货源/选品标题 */
    private String title;
    /** 选品封面 */
    private String coverUrl;
    /** 分类名 */
    private String categoryName;
    /** Giga 类目 id */
    private Long gigaCategoryId;
    /** 状态 */
    private Integer status;
    /** 文案标题 */
    private String contentTitle;
    /** 卖点 */
    private String contentSellingPoints;
    /** 生成图 URL（演示） */
    private String generatedImageUrl;
    /** 入库后的品库 ID */
    private Long productId;
    /** 入库后的 SKU */
    private String productSku;
    /** 认领人 */
    private Long assigneeUserId;
    /** 文案领取人 */
    private Long copyUserId;
    /** 美工人员 */
    private Long imageUserId;
    /** 上架平台 ID（原库 t_giga_listing_platform） */
    private String listingPlatformId;
    /** 上架店铺 ID（原库 t_giga_listing_shop） */
    private String listingShopId;
    /**
     * 工作流阶段：copy / image / list / done
     */
    private String workflowPhase;

}
