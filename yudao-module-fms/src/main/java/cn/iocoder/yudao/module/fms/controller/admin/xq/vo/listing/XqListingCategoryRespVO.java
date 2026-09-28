package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "上架分类（树节点）")
@Data
public class XqListingCategoryRespVO {

    private String id;
    private String name;
    private String parentId;
    private String platformId;
    private Integer sortOrder;
    private List<XqListingCategoryRespVO> children;

}
