package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "RPA AI - 生成图片提示词请求")
@Data
public class XqRpaAiImagePromptsReqVO {

    private String callbackToken;
    /** 识图结果：index / imageType / imageUrl / marker / bindIndexes */
    private List<Map<String, Object>> imagesMeta;
    /** title / sellingPoints / description / highlightStyle */
    private Map<String, Object> copyResult;
    /** promptText / negativePrompt / configJson */
    private Map<String, Object> imageRule;
    private Boolean mock;

}
