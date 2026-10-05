package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "类目字段默认值/AI映射")
@Data
public class XqCategoryFieldConfigRespVO {

    private String id;
    private String platformId;
    private String categoryId;
    private List<Item> fields = new ArrayList<>();

    @Data
    public static class Item {
        private String code;
        private String defaultValue;
        private String valueSource;
        private String zone;
    }

}
