package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 货源分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XqSourceItemPageReqVO extends PageParam {

    @Schema(description = "外部 SKU")
    private String externalSku;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "是否已认领")
    private Boolean claimed;

}
