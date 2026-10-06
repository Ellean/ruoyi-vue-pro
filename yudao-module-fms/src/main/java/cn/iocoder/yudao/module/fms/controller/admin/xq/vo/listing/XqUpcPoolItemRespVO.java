package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "UPC 库存项")
@Data
public class XqUpcPoolItemRespVO {
    private String id;
    private String upc;
    private String poolType;
    private String status;
    private String claimedByName;
    private LocalDateTime claimedAt;
    private LocalDateTime createDate;
}
