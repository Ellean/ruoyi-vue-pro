package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Giga 官网类目树节点")
@Data
public class XqGigaCategoryTreeRespVO {

    @Schema(description = "gigaId（前端用 id）")
    private Long id;
    private Long gigaId;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortOrder;
    private String imagePath;
    private String pathIds;
    private String pathNames;
    private String href;
    private List<XqGigaCategoryTreeRespVO> children = new ArrayList<>();

}
