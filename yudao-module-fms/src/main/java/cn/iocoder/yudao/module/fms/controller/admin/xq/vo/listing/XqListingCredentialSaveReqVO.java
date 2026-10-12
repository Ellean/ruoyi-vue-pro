package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "保存上架平台密钥")
@Data
public class XqListingCredentialSaveReqVO {
    private Long id;
    private Long platformId;
    private String sourcePlatformId;
    @NotBlank(message = "平台编码不能为空")
    private String platformCode;
    private String platformName;
    @NotBlank(message = "provider 不能为空")
    private String provider;
    private String baseUrl;
    private String shopId;
    /** 新建 Mirakl 必填；更新可空表示不改 */
    private String apiKey;
    private String clientId;
    private String clientSecret;
    private String supplierId;
    private String env;
    private String extraJson;
    private Boolean enabled;
    private String remark;
}
