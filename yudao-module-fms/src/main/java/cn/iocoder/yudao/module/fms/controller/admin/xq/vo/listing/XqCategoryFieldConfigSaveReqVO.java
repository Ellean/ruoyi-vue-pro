package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "保存类目字段默认值/AI映射")
@Data
public class XqCategoryFieldConfigSaveReqVO {

    @NotBlank(message = "平台不能为空")
    private String platformId;
    @NotBlank(message = "分类不能为空")
    private String categoryId;
    private List<Item> fields = new ArrayList<>();

    @Data
    public static class Item {
        private String code;
        private String defaultValue;
        private String valueSource;
        /** copy=产品文案 product=产品基本信息 attr=产品规格 other=其他属性 */
        private String zone;
    }

}
