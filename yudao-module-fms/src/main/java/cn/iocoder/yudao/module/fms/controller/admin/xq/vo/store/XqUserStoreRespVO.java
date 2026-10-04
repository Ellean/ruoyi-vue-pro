package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "XQ 用户绑店结果")
@Data
public class XqUserStoreRespVO {
    private Long userId;
    /** true=白名单；false=未绑定=不限店 */
    private Boolean restricted;
    private List<Long> storeIds;
}
