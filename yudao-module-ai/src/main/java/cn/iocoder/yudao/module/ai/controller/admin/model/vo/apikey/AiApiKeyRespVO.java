package cn.iocoder.yudao.module.ai.controller.admin.model.vo.apikey;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI API 密钥 Response VO")
@Data
public class AiApiKeyRespVO {

    private Long id;
    private String name;
    private String apiKey;
    private String platform;
    private String url;
    private String gatewayType;
    private String capabilities;
    private String chatModel;
    private String visionModel;
    private String imageModel;
    private String imageEditModel;
    private String imageBodyStyle;
    private Integer supportsAsync;
    private Integer preferResponsesApi;
    private String visionImageDetail;
    private String extraConfig;
    private String remark;
    private Integer status;

}
