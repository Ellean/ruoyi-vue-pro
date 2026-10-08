package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "批量分配美工")
@Data
public class XqWorkOrderAssignImageReqVO {

    @NotEmpty(message = "请选择任务")
    private List<Long> ids;

    @NotNull(message = "请选择美工人员")
    private Long imageUserId;

    @Schema(description = "上架平台（可选）")
    private String listingPlatformId;

    @Schema(description = "上架店铺（可选）")
    private String listingShopId;

    @Schema(description = "是否自动扩展为整家族（主体+全部变体）。任务列表外部分配默认 true；编辑页勾选分配传 false")
    private Boolean expandFamily;
}
