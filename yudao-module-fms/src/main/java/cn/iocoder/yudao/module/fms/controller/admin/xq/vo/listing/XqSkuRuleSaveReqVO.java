package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "保存 SKU 规则")
@Data
public class XqSkuRuleSaveReqVO {
    private String id;
    @Schema(description = "业务平台 ID（xq_platform.id）")
    private String platformId;
    @Schema(description = "业务店铺 ID（xq_store.id）")
    private String shopId;
    @Schema(description = "运营用户 ID，空=店铺通用")
    private String ownerUserId;
    private String name;
    private Boolean enabled;
    private Boolean skipSku;
    private List<Map<String, Object>> segments = new ArrayList<>();
}
