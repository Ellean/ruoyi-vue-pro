package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "价格增幅规则")
@Data
public class XqPriceMarkupRuleRespVO {
    private String id;
    private String platformId;
    private String platformName;
    private String shopId;
    private String shopName;
    private String ownerUserId;
    private String ownerUserName;
    private String name;
    private String mode;
    private BigDecimal value;
    private BigDecimal freightRate;
    private String applyTo;
    private Boolean enabled;
    private String summary;
    private BigDecimal samplePrice;
}
