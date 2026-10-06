package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "SKU 拼装规则")
@Data
public class XqSkuRuleRespVO {
    private String id;
    private String platformId;
    private String platformName;
    private String shopId;
    private String shopName;
    private String ownerUserId;
    private String ownerUserName;
    private String name;
    private List<Map<String, Object>> segments = new ArrayList<>();
    private Boolean enabled;
    private Boolean skipSku;
}
