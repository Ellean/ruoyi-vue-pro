package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "RPA AI - 生成文案请求")
@Data
public class XqRpaAiGenerateCopyReqVO {

    @Schema(description = "与 xq.rpa.callback-token 一致")
    private String callbackToken;

    private String sku;
    private String title;
    private String originalCopy;
    private List<String> sourceImages;
    /** { platformId, name, configJson } */
    private Map<String, Object> copyRule;
    /** 强制 mock（无密钥时也会自动 mock） */
    private Boolean mock;

}
