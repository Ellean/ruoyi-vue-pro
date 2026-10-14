package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.aiusage.XqAiUsageDashboardRespVO;
import cn.iocoder.yudao.module.fms.service.xq.XqAiUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - AI 用量计费")
@RestController
@RequestMapping("/xq/ai-usage")
@Validated
public class XqAiUsageController {

    @Resource
    private XqAiUsageService aiUsageService;

    @GetMapping("/dashboard")
    @Operation(summary = "AI 用量看板：文案/生图/改图费用（ToAPIs）")
    @Parameter(name = "startTimestamp", description = "窗口起点 Unix 秒")
    @Parameter(name = "endTimestamp", description = "窗口终点 Unix 秒")
    @PreAuthorize("@ss.hasAnyPermissions('xq:ai-usage:query', 'xq:work-order:query', 'ai:api-key:query')")
    public CommonResult<XqAiUsageDashboardRespVO> dashboard(
            @RequestParam(value = "startTimestamp", required = false) Long startTimestamp,
            @RequestParam(value = "endTimestamp", required = false) Long endTimestamp) {
        return success(aiUsageService.getDashboard(startTimestamp, endTimestamp));
    }
}
