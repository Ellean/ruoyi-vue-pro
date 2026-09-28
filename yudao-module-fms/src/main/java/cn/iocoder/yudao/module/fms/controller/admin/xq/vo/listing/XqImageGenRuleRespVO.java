package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "按分类图片提示词规则")
@Data
public class XqImageGenRuleRespVO {

    private String id;
    private String platformId;
    private String platformCode;
    private String platformName;
    private String categoryId;
    private String categoryName;
    private String name;
    private String promptText;
    private String negativePrompt;
    private String configJson;
    private Boolean enabled;
    private String remark;

}
