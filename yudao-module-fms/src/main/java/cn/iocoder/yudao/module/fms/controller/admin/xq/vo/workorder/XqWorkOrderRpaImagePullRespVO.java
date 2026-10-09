package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Schema(description = "RPA：领取待跑生图任务 Response")
@Data
public class XqWorkOrderRpaImagePullRespVO {

    private Integer count;
    private List<Map<String, Object>> jobs = new ArrayList<>();

}
