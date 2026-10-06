package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "平台字段池（杂糅全部原分类字段）")
@Data
public class XqFieldPoolRespVO {

    private String platformId;
    private Integer total;
    private Integer requiredCount;
    private Integer categoryCount;
    /** catalog=原库分类模板 amazon-default=亚马逊内置字段 */
    private String sourceHint;
    private List<XqCategoryFieldTemplateRespVO.Field> fields = new ArrayList<>();
    private List<XqShopFieldConfigRespVO.Partition> partitions = new ArrayList<>();

}
