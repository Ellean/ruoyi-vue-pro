package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "UPC 规则")
@Data
public class XqUpcRuleRespVO {
    private String id;
    private String platformId;
    private String platformName;
    private String shopId;
    private String shopName;
    private String name;
    private String mode;
    private String flagDigit;
    private String manufacturerCode;
    private Integer productWidth;
    private Integer productStart;
    private Boolean enabled;
    private Long poolFreeAvailable;
    private Long poolPaidAvailable;
    private Long poolUsed;
}
