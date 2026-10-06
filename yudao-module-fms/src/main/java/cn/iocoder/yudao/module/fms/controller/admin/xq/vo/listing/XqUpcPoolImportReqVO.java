package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "导入 UPC")
@Data
public class XqUpcPoolImportReqVO {
    /** 粘贴文本或 codes 列表 */
    private String text;
    private List<String> codes;
    /** free / paid */
    private String poolType;
}
