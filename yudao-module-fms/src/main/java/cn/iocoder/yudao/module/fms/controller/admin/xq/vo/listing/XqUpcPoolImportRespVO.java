package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "导入 UPC 结果")
@Data
public class XqUpcPoolImportRespVO {
    private Integer imported;
    private Integer skipped;
    private List<String> invalid = new ArrayList<>();
    private String poolType;
}
