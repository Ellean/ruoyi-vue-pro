package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "保存价格增幅规则")
@Data
public class XqPriceMarkupRuleSaveReqVO {
    private String id;
    private String platformId;
    private String shopId;
    private String ownerUserId;
    private String name;
    /** fixed / percent / ratio */
    private String mode;
    private BigDecimal value;
    private BigDecimal freightRate;
    /** dropship / pickup / both */
    private String applyTo;
    private Boolean enabled;
}
