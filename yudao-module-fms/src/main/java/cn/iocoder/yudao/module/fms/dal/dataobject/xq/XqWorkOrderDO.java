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
    /** Giga 产品 list id */
    private String gigaProductId;
    /** 主体任务 ID（变体指向主体；主体为空） */
    private Long parentWorkOrderId;
    /** 主体 SKU */
    private String parentSku;
    /** 变体名称/颜色 */
    private String variantLabel;
    /** 外部 SKU / Item Code */
    private String externalSku;
    /** 货源/选品标题 */
    private String title;
    /** 选品封面 */
    private String coverUrl;
    /** Giga 原文案/描述 */
    private String sourceDescription;
    /** 原图 URL JSON 数组 */
    private String sourceImageUrls;
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
    /** 生成文案结构 JSON */
    private String copyResultJson;
    /** 图片提示词条目 JSON */
    private String imagePromptJson;
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
    /** 图片作业：todo / rejected / revised / done */
    private String imageStatus;
    /** 上架平台 ID（原库 t_giga_listing_platform） */
    private String listingPlatformId;
    /** 上架店铺 ID（原库 t_giga_listing_shop） */
    private String listingShopId;
    /** 上架分类 ID（原库 t_giga_listing_category） */
    private String listingCategoryId;
    /** 上架平台名（冗余，便于列表筛选展示） */
    private String listingPlatformName;
    /** 上架店铺名 */
    private String listingShopName;
    /** 上架分类名 */
    private String listingCategoryName;
    /** 上架国家代码 */
    private String listingCountryCode;
    /** 上架国家名 */
    private String listingCountryName;
    /** 上架模板字段值 JSON */
    private String listingValuesJson;
    /** 上架调用结果 */
    private String listingResultJson;
    /**
     * 上架子状态：export_pending_confirm（导表成功待确认）/ listed
     */
    private String listingStatus;
    /**
     * 工作流阶段：copy / image / list / done / closed
     */
    private String workflowPhase;
    /** 文案 RPA workUuid */
    private String rpaCopyWorkUuid;
    /** idle/queued/running/success/fail */
    private String rpaCopyStatus;
    /** 文案 RPA 失败原因 */
    private String rpaCopyError;
    /**
     * 是否勾选独立跑文案 RPA。
     * 主体默认 true；变体勾选后独立入队；未勾选在主体成功后沿用主体文案。
     */
    private Boolean copyRpaSelected;
    /** 生图 RPA workUuid */
    private String rpaImageWorkUuid;
    /** idle/queued/running/success/fail */
    private String rpaImageStatus;
    /** 生图 RPA 失败原因 */
    private String rpaImageError;

}
