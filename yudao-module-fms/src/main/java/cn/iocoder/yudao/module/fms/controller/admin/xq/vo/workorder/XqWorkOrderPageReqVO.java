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

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "认领人", hidden = true)
    private Long assigneeUserId;

}
