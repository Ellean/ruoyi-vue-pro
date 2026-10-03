package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "RPA 待跑文案列表")
@Data
public class XqWorkOrderRpaCopyPullRespVO {

    private Integer count;
    /** 精简列表：workOrderId / sku / title，详情再按 SKU 拉 */
    private List<Map<String, Object>> jobs = new ArrayList<>();

}
