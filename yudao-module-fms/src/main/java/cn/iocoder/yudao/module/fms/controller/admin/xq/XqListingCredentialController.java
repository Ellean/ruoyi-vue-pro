package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCredentialRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCredentialSaveReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqListingCredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - 上架平台密钥")
@RestController
@RequestMapping("/xq/listing-credential")
@Validated
public class XqListingCredentialController {

    @Resource
    private XqListingCredentialService listingCredentialService;

    @GetMapping("/list")
    @Operation(summary = "上架平台密钥列表（敏感字段仅掩码）")
    @PreAuthorize("@ss.hasPermission('xq:listing-credential:query')")
    public CommonResult<List<XqListingCredentialRespVO>> list() {
        return success(listingCredentialService.list());
    }

    @PostMapping("/save")
    @Operation(summary = "创建或更新上架平台密钥")
    @PreAuthorize("@ss.hasPermission('xq:listing-credential:create') or @ss.hasPermission('xq:listing-credential:update')")
    public CommonResult<Long> save(@Valid @RequestBody XqListingCredentialSaveReqVO reqVO) {
        return success(listingCredentialService.save(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除上架平台密钥")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('xq:listing-credential:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        listingCredentialService.delete(id);
        return success(true);
    }

    @PostMapping("/sync-from-xq")
    @Operation(summary = "从原库同步 Mirakl/Wayfair 密钥")
    @PreAuthorize("@ss.hasPermission('xq:listing-credential:sync')")
    public CommonResult<Map<String, Object>> syncFromXq() {
        return success(listingCredentialService.syncFromXq());
    }

}
