package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "RPA：生图/改图（替代旧 ERP Aixoras）")
@Data
public class XqRpaAiGenerateImageReqVO {

    @Schema(description = "与后台 xq.rpa.callback-token 一致")
    private String callbackToken;

    @Schema(description = "提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    private String prompt;

    @Schema(description = "SKU")
    private String sku;

    /**
     * 参考图：{ contentBase64|base64|data, mimeType|mime, fileName|name, url }
     */
    private List<Map<String, Object>> images;

    @Schema(description = "超时毫秒")
    private Integer timeoutMs;

    @Schema(description = "是否回传 base64（默认 false，只回 url）")
    private Boolean returnBase64;

    @Schema(description = "是否锁产品外形（默认 true）")
    private Boolean lockProduct;

}
