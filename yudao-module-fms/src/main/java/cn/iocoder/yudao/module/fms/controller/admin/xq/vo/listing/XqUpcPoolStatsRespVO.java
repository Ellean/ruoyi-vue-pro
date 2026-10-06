package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "UPC 池统计")
@Data
public class XqUpcPoolStatsRespVO {
    private Long freeAvailable;
    private Long paidAvailable;
    private Long used;
    private Long voidCount;
}
