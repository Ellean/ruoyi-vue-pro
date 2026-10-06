package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCategoryFieldConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCategoryFieldConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCategoryFieldTemplateRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqFieldPoolRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqShopFieldConfigRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqShopFieldConfigSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleSaveReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingCategoryRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingPlatformRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqListingShopRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqWorkbenchScopeRespVO;
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
    @Operation(summary = "上架平台列表（主库 xq_platform，按用户绑店过滤）")
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:template-config:query', 'xq:product:dispatch', 'xq:work-order:query', 'xq:work-order:list')")
    public CommonResult<List<XqListingPlatformRespVO>> listPlatforms() {
        return success(listingCatalogService.listPlatforms());
    }

    @GetMapping("/shops")
    @Operation(summary = "上架店铺列表（主库 xq_store，按 xq_user_store 过滤）")
    @Parameter(name = "platformId", description = "业务平台ID（xq_platform.id）")
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:template-config:query', 'xq:product:dispatch', 'xq:work-order:query', 'xq:work-order:list')")
    public CommonResult<List<XqListingShopRespVO>> listShops(
            @RequestParam(value = "platformId", required = false) String platformId) {
        return success(listingCatalogService.listShops(platformId));
    }

    @GetMapping("/workbench-scope")
    @Operation(summary = "平台工作台：当前用户绑定的平台与店铺")
    @PreAuthorize("@ss.hasAnyPermissions('xq:work-order:query', 'xq:work-order:list', 'xq:template-config:query', 'xq:product:dispatch')")
    public CommonResult<XqWorkbenchScopeRespVO> getWorkbenchScope() {
        return success(listingCatalogService.getWorkbenchScope());
    }

    @GetMapping("/categories")
    @Operation(summary = "上架分类树（按业务平台解析 sourcePlatformId 后查原库类目）")
    @Parameter(name = "platformId", description = "业务平台ID（xq_platform.id）", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:template-config:query', 'xq:product:dispatch', 'xq:work-order:query', 'xq:work-order:list')")
    public CommonResult<List<XqListingCategoryRespVO>> listCategories(
            @RequestParam("platformId") String platformId) {
        return success(listingCatalogService.listCategoryTree(platformId));
    }

    @GetMapping("/category-template")
    @Operation(summary = "源库类目字段模板（Mirakl/分类属性，不是文案规则）")
    @Parameter(name = "categoryId", description = "上架分类ID", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:image-rule:query', 'xq:template-config:query', 'xq:product:dispatch', 'xq:work-order:query', 'xq:work-order:update', 'xq:work-order:list')")
    public CommonResult<XqCategoryFieldTemplateRespVO> getCategoryFieldTemplate(
            @RequestParam("categoryId") String categoryId) {
        return success(listingCatalogService.getCategoryFieldTemplate(categoryId));
    }

    @GetMapping("/copy-rules")
    @Operation(summary = "各平台文案规则列表")
    @PreAuthorize("@ss.hasPermission('xq:copy-rule:query')")
    public CommonResult<List<XqCopyGenRuleRespVO>> listCopyRules() {
        return success(listingCatalogService.listCopyRules());
    }

    @GetMapping("/copy-rule")
    @Operation(summary = "获得平台文案规则")
    @PreAuthorize("@ss.hasAnyPermissions('xq:copy-rule:query', 'xq:work-order:query', 'xq:work-order:update')")
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

    @GetMapping("/category-field-configs")
    @Operation(summary = "列出某平台已配置字段默认值/AI映射的分类")
    @PreAuthorize("@ss.hasPermission('xq:template-config:query')")
    public CommonResult<List<XqCategoryFieldConfigRespVO>> listCategoryFieldConfigs(
            @RequestParam("platformId") String platformId) {
        return success(listingCatalogService.listCategoryFieldConfigs(platformId));
    }

    @GetMapping("/category-field-config")
    @Operation(summary = "获得分类字段默认值与AI映射")
    @PreAuthorize("@ss.hasAnyPermissions('xq:template-config:query', 'xq:work-order:query', 'xq:work-order:update')")
    public CommonResult<XqCategoryFieldConfigRespVO> getCategoryFieldConfig(
            @RequestParam("platformId") String platformId,
            @RequestParam("categoryId") String categoryId) {
        return success(listingCatalogService.getCategoryFieldConfig(platformId, categoryId));
    }

    @PostMapping("/category-field-config")
    @Operation(summary = "保存分类字段默认值与AI映射（写回原库）")
    @PreAuthorize("@ss.hasPermission('xq:template-config:update')")
    public CommonResult<Boolean> saveCategoryFieldConfig(
            @Valid @RequestBody XqCategoryFieldConfigSaveReqVO reqVO) {
        listingCatalogService.saveCategoryFieldConfig(reqVO);
        return success(true);
    }

    @GetMapping("/field-pool")
    @Operation(summary = "平台字段池（杂糅全部原分类模板字段），可叠加店铺+国家分区")
    @PreAuthorize("@ss.hasAnyPermissions('xq:template-config:query', 'xq:work-order:query', 'xq:work-order:update')")
    public CommonResult<XqFieldPoolRespVO> getFieldPool(
            @RequestParam("platformId") String platformId,
            @RequestParam(value = "shopId", required = false) String shopId,
            @RequestParam(value = "country", required = false) String country) {
        return success(listingCatalogService.getFieldPool(platformId, shopId, country));
    }

    @GetMapping("/shop-field-config")
    @Operation(summary = "获得店铺+国家字段池配置")
    @PreAuthorize("@ss.hasAnyPermissions('xq:template-config:query', 'xq:work-order:query', 'xq:work-order:update')")
    public CommonResult<XqShopFieldConfigRespVO> getShopFieldConfig(
            @RequestParam("platformId") String platformId,
            @RequestParam("shopId") String shopId,
            @RequestParam("country") String country) {
        return success(listingCatalogService.getShopFieldConfig(platformId, shopId, country));
    }

    @PostMapping("/shop-field-config")
    @Operation(summary = "保存店铺+国家字段池与分区")
    @PreAuthorize("@ss.hasPermission('xq:template-config:update')")
    public CommonResult<Boolean> saveShopFieldConfig(
            @Valid @RequestBody XqShopFieldConfigSaveReqVO reqVO) {
        listingCatalogService.saveShopFieldConfig(reqVO);
        return success(true);
    }

}
