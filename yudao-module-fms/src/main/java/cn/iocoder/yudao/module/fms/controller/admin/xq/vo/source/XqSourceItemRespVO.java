package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 货源 Response VO")
@Data
public class XqSourceItemRespVO {

    private Long id;
    private String externalSku;
    private String title;
    private BigDecimal price;
    private Integer stock;
    private String sourceName;
    private String imageUrl;
    private Boolean claimed;
    private LocalDateTime createTime;

}
