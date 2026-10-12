package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent.XqAgentAiTestPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent.XqAgentAiTestRespVO;
import cn.iocoder.yudao.module.fms.service.xq.XqAgentAiTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - AI图结果测试页")
@RestController
@RequestMapping("/xq/agent-ai-test")
@Validated
public class XqAgentAiTestController {

    @Resource
    private XqAgentAiTestService agentAiTestService;

    @GetMapping("/page")
    @Operation(summary = "本地已同步 AI 图分页（只读测试）")
    @PreAuthorize("@ss.hasPermission('xq:agent-ai-test:query')")
    public CommonResult<PageResult<XqAgentAiTestRespVO>> page(@Valid XqAgentAiTestPageReqVO reqVO) {
        return success(agentAiTestService.getPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "本地 AI 图详情（含图片列表）")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('xq:agent-ai-test:query')")
    public CommonResult<XqAgentAiTestRespVO> get(@RequestParam("id") String id) {
        return success(agentAiTestService.get(id));
    }

}
