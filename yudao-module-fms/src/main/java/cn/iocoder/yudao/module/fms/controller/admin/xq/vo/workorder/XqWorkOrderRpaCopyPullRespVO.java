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
    /** 当前登录用户（便于排查 copy_user_id 不一致） */
    private Long loginUserId;
    /** 当前租户 */
    private Long tenantId;
    /** 文案领取人=我 且 queued */
    private Long queuedForUser;
    /** 工作台上我能看到的 queued（认领人或文案领取人是我） */
    private Long queuedOnDesk;
    /** 全表 queued（表无租户隔离，别人的排队，不是我的） */
    private Long queuedAll;
    /** 探测：工作台上进行中任务的实际状态（不要求 queued） */
    private List<Map<String, Object>> deskSample = new ArrayList<>();

}
