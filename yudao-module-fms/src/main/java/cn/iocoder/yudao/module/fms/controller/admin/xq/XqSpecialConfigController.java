package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqPriceMarkupRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqPriceMarkupRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqSkuRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqSkuRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolImportReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolImportRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolItemRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcPoolStatsRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqUpcRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqSpecialConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - 特殊配置（SKU / 价格增幅 / UPC）")
@RestController
@RequestMapping("/xq/listing")
@Validated
public class XqSpecialConfigController {

    private static final String QUERY = "@ss.hasAnyPermissions('xq:template-config:query', 'xq:work-order:query', 'xq:work-order:list')";
    private static final String UPDATE = "@ss.hasAnyPermissions('xq:template-config:update', 'xq:work-order:update')";

    @Resource
    private XqSpecialConfigService specialConfigService;

    @GetMapping("/sku-rules")
    @Operation(summary = "SKU 拼装规则列表")
    @PreAuthorize(QUERY)
    public CommonResult<List<XqSkuRuleRespVO>> listSkuRules(
            @RequestParam("platformId") String platformId,
            @RequestParam(value = "shopId", required = false) String shopId) {
        return success(specialConfigService.listSkuRules(platformId, shopId));
    }

    @PostMapping("/sku-rule")
    @Operation(summary = "保存 SKU 拼装规则")
    @PreAuthorize(UPDATE)
    public CommonResult<XqSkuRuleRespVO> saveSkuRule(@Valid @RequestBody XqSkuRuleSaveReqVO reqVO) {
        return success(specialConfigService.saveSkuRule(reqVO));
    }

    @DeleteMapping("/sku-rule")
    @Operation(summary = "删除 SKU 拼装规则")
    @PreAuthorize(UPDATE)
    public CommonResult<Boolean> deleteSkuRule(@RequestParam("id") String id) {
        specialConfigService.deleteSkuRule(id);
        return success(true);
    }

    @GetMapping("/price-markup-rules")
    @Operation(summary = "价格增幅规则列表")
    @PreAuthorize(QUERY)
    public CommonResult<List<XqPriceMarkupRuleRespVO>> listMarkupRules(
            @RequestParam("platformId") String platformId,
            @RequestParam(value = "shopId", required = false) String shopId) {
        return success(specialConfigService.listMarkupRules(platformId, shopId));
    }

    @PostMapping("/price-markup-rule")
    @Operation(summary = "保存价格增幅规则")
    @PreAuthorize(UPDATE)
    public CommonResult<XqPriceMarkupRuleRespVO> saveMarkupRule(@Valid @RequestBody XqPriceMarkupRuleSaveReqVO reqVO) {
        return success(specialConfigService.saveMarkupRule(reqVO));
    }

    @DeleteMapping("/price-markup-rule")
    @Operation(summary = "删除价格增幅规则")
    @PreAuthorize(UPDATE)
    public CommonResult<Boolean> deleteMarkupRule(@RequestParam("id") String id) {
        specialConfigService.deleteMarkupRule(id);
        return success(true);
    }

    @GetMapping("/upc-rules")
    @Operation(summary = "UPC 规则列表")
    @PreAuthorize(QUERY)
    public CommonResult<List<XqUpcRuleRespVO>> listUpcRules(@RequestParam("platformId") String platformId) {
        return success(specialConfigService.listUpcRules(platformId));
    }

    @PostMapping("/upc-rule")
    @Operation(summary = "保存 UPC 规则")
    @PreAuthorize(UPDATE)
    public CommonResult<XqUpcRuleRespVO> saveUpcRule(@Valid @RequestBody XqUpcRuleSaveReqVO reqVO) {
        return success(specialConfigService.saveUpcRule(reqVO));
    }

    @DeleteMapping("/upc-rule")
    @Operation(summary = "删除 UPC 规则")
    @PreAuthorize(UPDATE)
    public CommonResult<Boolean> deleteUpcRule(@RequestParam("id") String id) {
        specialConfigService.deleteUpcRule(id);
        return success(true);
    }

    @GetMapping("/upc-pool/stats")
    @Operation(summary = "全局 UPC 池统计")
    @PreAuthorize(QUERY)
    public CommonResult<XqUpcPoolStatsRespVO> upcPoolStats() {
        return success(specialConfigService.getUpcPoolStats());
    }

    @GetMapping("/upc-pool")
    @Operation(summary = "全局 UPC 池分页")
    @PreAuthorize(QUERY)
    public CommonResult<PageResult<XqUpcPoolItemRespVO>> pageUpcPool(
            @RequestParam(value = "poolType", required = false) String poolType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        return success(specialConfigService.pageUpcPool(poolType, status, pageNo, pageSize));
    }

    @PostMapping("/upc-pool/import")
    @Operation(summary = "导入全局 UPC")
    @PreAuthorize(UPDATE)
    public CommonResult<XqUpcPoolImportRespVO> importUpcPool(@RequestBody XqUpcPoolImportReqVO reqVO) {
        return success(specialConfigService.importUpcPool(reqVO));
    }

    @PostMapping("/upc-pool/void")
    @Operation(summary = "作废 UPC")
    @PreAuthorize(UPDATE)
    public CommonResult<Boolean> voidUpc(@RequestParam("id") String id) {
        specialConfigService.voidUpcPoolItem(id);
        return success(true);
    }

    @PostMapping("/upc-pool/release")
    @Operation(summary = "释放 UPC 回池")
    @PreAuthorize(UPDATE)
    public CommonResult<Boolean> releaseUpc(@RequestParam("id") String id) {
        specialConfigService.releaseUpcPoolItem(id);
        return success(true);
    }

}
