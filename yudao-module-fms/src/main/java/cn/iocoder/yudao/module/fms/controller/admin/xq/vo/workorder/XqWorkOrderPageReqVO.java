package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 作业单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XqWorkOrderPageReqVO extends PageParam {

    @Schema(description = "作业号")
    private String no;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "Item Code / SKU")
    private String externalSku;

    @Schema(description = "关键词（任务号/标题/SKU）")
    private String keyword;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "Giga 类目 id")
    private Long gigaCategoryId;

    @Schema(description = "文案领取人")
    private Long copyUserId;

    @Schema(description = "美工人员")
    private Long imageUserId;

    @Schema(description = "是否已完成文案")
    private Boolean copyReady;

    @Schema(description = "工作流阶段 copy/image/list/done")
    private String workflowPhase;

    @Schema(description = "上架平台 ID")
    private String listingPlatformId;

    @Schema(description = "上架店铺 ID")
    private String listingShopId;

    @Schema(description = "上架分类 ID")
    private String listingCategoryId;

    @Schema(description = "认领人", hidden = true)
    private Long assigneeUserId;

    @Schema(description = "我的文案：文案领取人或下发人")
    private Long mineUserId;

    @Schema(description = "我的图片：美工本人，或分配人（仍能看到已分配任务）")
    private Long mineImageUserId;

    @Schema(description = "图片作业状态 todo/rejected/revised/done")
    private String imageStatus;

    @Schema(description = "待上架：图片已出图/已完成且尚未提交")
    private Boolean listingReady;

}
