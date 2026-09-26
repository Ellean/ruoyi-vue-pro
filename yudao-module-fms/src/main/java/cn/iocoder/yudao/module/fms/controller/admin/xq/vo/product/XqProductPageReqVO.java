package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 品库产品分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class XqProductPageReqVO extends PageParam {

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "名称 / Item Code 关键字")
    private String name;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "Giga 类目 ID（含子树）")
    private Long gigaCategoryId;

}
