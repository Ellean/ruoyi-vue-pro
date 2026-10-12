package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPA：上传成品图（替代旧 ERP OSS/Postimages）")
@Data
public class XqRpaAiUploadImageReqVO {

    @Schema(description = "与后台 xq.rpa.callback-token 一致")
    private String callbackToken;

    @Schema(description = "SKU")
    private String sku;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "MIME，如 image/jpeg")
    private String mimeType;

    @Schema(description = "图片 base64（可带 data:image/...;base64, 前缀）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentBase64;

}
