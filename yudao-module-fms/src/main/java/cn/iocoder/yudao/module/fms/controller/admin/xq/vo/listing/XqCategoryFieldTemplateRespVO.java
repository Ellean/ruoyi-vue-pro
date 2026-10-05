package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "上架类目字段模板（源库 Mirakl/分类模板，不含文案规则）")
@Data
public class XqCategoryFieldTemplateRespVO {

    private String id;
    private String categoryId;
    private String platformId;
    private String hierarchyCode;
    private String name;
    private Integer total;
    private Integer requiredCount;
    private Integer recommendedCount;
    private Integer optionalCount;
    private List<Field> fields = new ArrayList<>();

    @Data
    public static class Field {
        private String code;
        private String label;
        private String type;
        private String requirementLevel;
        private Boolean required;
        private String source;
        private String groupLabel;
        private String bandLabel;
        private String valuesList;
        /** 该类目配置的字段默认值 */
        private String defaultValue;
        /** 该类目配置的 AI/商品映射，如 ai_title */
        private String valueSource;
    }

}
