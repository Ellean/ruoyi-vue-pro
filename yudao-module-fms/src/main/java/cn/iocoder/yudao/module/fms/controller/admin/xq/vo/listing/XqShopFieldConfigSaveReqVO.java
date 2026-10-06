package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "保存店铺+国家字段池配置")
@Data
public class XqShopFieldConfigSaveReqVO {

    @NotBlank(message = "平台不能为空")
    private String platformId;
    @NotBlank(message = "店铺不能为空")
    private String shopId;
    @NotBlank(message = "国家不能为空")
    private String country;
    private List<Item> fields = new ArrayList<>();
    private List<XqShopFieldConfigRespVO.Partition> partitions = new ArrayList<>();

    @Data
    public static class Item {
        private String code;
        private String defaultValue;
        private String valueSource;
        private String zone;
        private Boolean required;
    }

}
