package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "平台工作台范围（当前用户绑店）")
@Data
public class XqWorkbenchScopeRespVO {

    private List<Platform> platforms = new ArrayList<>();

    @Data
    public static class Platform {
        private String id;
        private String code;
        private String name;
        private Integer sortOrder;
        private List<XqListingShopRespVO> shops = new ArrayList<>();
    }

}
