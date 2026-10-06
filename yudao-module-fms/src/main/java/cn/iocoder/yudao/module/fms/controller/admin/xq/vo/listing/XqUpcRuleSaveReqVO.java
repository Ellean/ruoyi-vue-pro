package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "保存 UPC 规则")
@Data
public class XqUpcRuleSaveReqVO {
    private String id;
    private String platformId;
    private String shopId;
    private String name;
    /** none / auto / pool */
    private String mode;
    private String flagDigit;
    private String manufacturerCode;
    private Integer productWidth;
    private Integer productStart;
    private Boolean enabled;
}
