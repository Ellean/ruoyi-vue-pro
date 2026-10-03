package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga.XqGigaCredentialRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.giga.XqGigaCredentialSaveReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqGigaCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - 产品库 Giga 凭证")
@RestController
@RequestMapping("/xq/giga-credential")
@Validated
public class XqGigaCredentialController {

    @Resource
    private XqGigaCredentialService gigaCredentialService;

    @GetMapping("/list")
    @Operation(summary = "产品库凭证列表（Secret 仅掩码）")
    @PreAuthorize("@ss.hasPermission('xq:giga-credential:query')")
    public CommonResult<List<XqGigaCredentialRespVO>> list() {
        return success(gigaCredentialService.list());
    }

    @PostMapping("/save")
    @Operation(summary = "创建或更新产品库凭证")
    @PreAuthorize("@ss.hasPermission('xq:giga-credential:create') or @ss.hasPermission('xq:giga-credential:update')")
    public CommonResult<Long> save(@Valid @RequestBody XqGigaCredentialSaveReqVO reqVO) {
        return success(gigaCredentialService.save(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品库凭证")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('xq:giga-credential:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        gigaCredentialService.delete(id);
        return success(true);
    }

    @PostMapping("/set-default")
    @Operation(summary = "设为默认凭证")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('xq:giga-credential:update')")
    public CommonResult<Boolean> setDefault(@RequestParam("id") Long id) {
        gigaCredentialService.setDefault(id);
        return success(true);
    }

}
