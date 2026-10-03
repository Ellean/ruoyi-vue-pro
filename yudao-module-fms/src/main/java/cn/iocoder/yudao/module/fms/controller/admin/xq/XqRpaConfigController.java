package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.service.xq.XqRpaConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 工作台 RPA 配置")
@RestController
@RequestMapping("/xq/rpa-config")
@Validated
public class XqRpaConfigController {

    @Resource
    private XqRpaConfigService rpaConfigService;

    @GetMapping("/get")
    @Operation(summary = "获得当前用户 RPA 配置")
    @PreAuthorize("@ss.hasPermission('xq:rpa-config:query')")
    public CommonResult<XqRpaConfigRespVO> getConfig() {
        return success(rpaConfigService.getConfig(getLoginUserId()));
    }

    @PostMapping("/save")
    @Operation(summary = "保存当前用户 RPA 配置")
    @PreAuthorize("@ss.hasPermission('xq:rpa-config:update')")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody XqRpaConfigSaveReqVO reqVO) {
        rpaConfigService.saveConfig(getLoginUserId(), reqVO);
        return success(true);
    }

    @PostMapping("/trigger")
    @Operation(summary = "触发控制中枢 RPA（copy/image）")
    @PreAuthorize("@ss.hasPermission('xq:rpa-config:trigger')")
    public CommonResult<XqRpaTriggerRespVO> trigger(@Valid @RequestBody XqRpaTriggerReqVO reqVO) {
        return success(rpaConfigService.trigger(getLoginUserId(), reqVO));
    }

}
