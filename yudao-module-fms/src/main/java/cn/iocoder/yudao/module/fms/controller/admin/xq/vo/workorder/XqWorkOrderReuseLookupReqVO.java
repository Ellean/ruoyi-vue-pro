package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 下发前反查可复用文案/图片 Request VO")
@Data
public class XqWorkOrderReuseLookupReqVO {

    @Schema(description = "待下发 SKU 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "SKU 不能为空")
    private List<String> skus;

    @Schema(description = "排除平台 ID（即将下发的目标平台，只看其他平台）")
    private List<String> excludePlatformIds;

}
