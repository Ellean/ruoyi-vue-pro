package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCategoryRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingPlatformRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingShopRespVO;
import cn.iocoder.yudao.module.fms.service.xq.XqListingCatalogService;
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

@Tag(name = "产品 - 上架平台/类目/文案规则")
@RestController
@RequestMapping("/xq/listing")
@Validated
public class XqListingCatalogController {

    @Resource
    private XqListingCatalogService listingCatalogService;

    @GetMapping("/platforms")
    @Operation(summary = "上架平台列表（原库）")
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:product:dispatch', 'xq:work-order:query')")
    public CommonResult<List<XqListingPlatformRespVO>> listPlatforms() {
        return success(listingCatalogService.listPlatforms());
    }

    @GetMapping("/shops")
    @Operation(summary = "上架店铺列表（原库）")
    @Parameter(name = "platformId", description = "平台ID")
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:product:dispatch', 'xq:work-order:query')")
    public CommonResult<List<XqListingShopRespVO>> listShops(
            @RequestParam(value = "platformId", required = false) String platformId) {
        return success(listingCatalogService.listShops(platformId));
    }

    @GetMapping("/categories")
    @Operation(summary = "上架分类树（原库，按平台）")
    @Parameter(name = "platformId", description = "平台ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:product:dispatch', 'xq:work-order:query')")
    public CommonResult<List<XqListingCategoryRespVO>> listCategories(
            @RequestParam("platformId") String platformId) {
        return success(listingCatalogService.listCategoryTree(platformId));
    }

    @GetMapping("/copy-rules")
    @Operation(summary = "各平台文案规则列表")
    @PreAuthorize("@ss.hasPermission('xq:copy-rule:query')")
    public CommonResult<List<XqCopyGenRuleRespVO>> listCopyRules() {
        return success(listingCatalogService.listCopyRules());
    }

    @GetMapping("/copy-rule")
    @Operation(summary = "获得平台文案规则")
    @PreAuthorize("@ss.hasPermission('xq:copy-rule:query')")
    public CommonResult<XqCopyGenRuleRespVO> getCopyRule(
            @RequestParam(value = "platformId", required = false, defaultValue = "") String platformId) {
        return success(listingCatalogService.getCopyRule(platformId));
    }

    @PostMapping("/copy-rule")
    @Operation(summary = "保存平台文案规则（写回原库）")
    @PreAuthorize("@ss.hasPermission('xq:copy-rule:update')")
    public CommonResult<Boolean> saveCopyRule(@Valid @RequestBody XqCopyGenRuleSaveReqVO reqVO) {
        listingCatalogService.saveCopyRule(reqVO);
        return success(true);
    }

    @GetMapping("/image-rules")
    @Operation(summary = "按平台列出已配置的分类图片提示词")
    @Parameter(name = "platformId", description = "平台ID，空=通用")
    @PreAuthorize("@ss.hasAnyPermissions('xq:image-rule:query', 'xq:work-order:gen-image')")
    public CommonResult<List<XqImageGenRuleRespVO>> listImageRules(
            @RequestParam(value = "platformId", required = false, defaultValue = "") String platformId) {
        return success(listingCatalogService.listImageRules(platformId));
    }

    @GetMapping("/image-rule")
    @Operation(summary = "获得平台+分类图片提示词")
    @PreAuthorize("@ss.hasAnyPermissions('xq:image-rule:query', 'xq:work-order:gen-image')")
    public CommonResult<XqImageGenRuleRespVO> getImageRule(
            @RequestParam(value = "platformId", required = false, defaultValue = "") String platformId,
            @RequestParam(value = "categoryId", required = false, defaultValue = "") String categoryId) {
        return success(listingCatalogService.getImageRule(platformId, categoryId));
    }

    @PostMapping("/image-rule")
    @Operation(summary = "保存平台+分类图片提示词（写回原库）")
    @PreAuthorize("@ss.hasPermission('xq:image-rule:update')")
    public CommonResult<Boolean> saveImageRule(@Valid @RequestBody XqImageGenRuleSaveReqVO reqVO) {
        listingCatalogService.saveImageRule(reqVO);
        return success(true);
    }

}
