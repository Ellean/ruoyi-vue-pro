package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "店铺+国家字段配置")
@Data
public class XqShopFieldConfigRespVO {

    private String id;
    private String platformId;
    private String shopId;
    private String country;
    private List<Item> fields = new ArrayList<>();
    private List<Partition> partitions = new ArrayList<>();

    @Data
    public static class Item {
        private String code;
        private String defaultValue;
        private String valueSource;
        private String zone;
        private Boolean required;
    }

    @Data
    public static class Partition {
        private String id;
        private String name;
        private String keywords;
        private Boolean system;
        private String color;
    }

}
