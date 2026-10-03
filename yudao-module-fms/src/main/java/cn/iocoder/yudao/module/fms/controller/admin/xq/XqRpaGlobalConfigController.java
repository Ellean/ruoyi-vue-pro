package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaGlobalConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaGlobalConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqRpaGlobalConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - 系统管理全局密钥")
@RestController
@RequestMapping("/xq/rpa-global")
@Validated
public class XqRpaGlobalConfigController {

    @Resource
    private XqRpaGlobalConfigService rpaGlobalConfigService;

    @GetMapping("/get")
    @Operation(summary = "获得控制中枢全局密钥（掩码）")
    @PreAuthorize("@ss.hasPermission('xq:rpa-global:query')")
    public CommonResult<XqRpaGlobalConfigRespVO> get() {
        return success(rpaGlobalConfigService.get());
    }

    @PostMapping("/save")
    @Operation(summary = "保存控制中枢全局密钥")
    @PreAuthorize("@ss.hasPermission('xq:rpa-global:update')")
    public CommonResult<Boolean> save(@Valid @RequestBody XqRpaGlobalConfigSaveReqVO reqVO) {
        rpaGlobalConfigService.save(reqVO);
        return success(true);
    }

}
