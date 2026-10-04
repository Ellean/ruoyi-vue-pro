package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.store.*;
import cn.iocoder.yudao.module.fms.service.xq.XqStorePlatformService;
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

@Tag(name = "产品 - 店铺平台维护")
@RestController
@RequestMapping("/xq/store-platform")
@Validated
public class XqStorePlatformController {

    @Resource
    private XqStorePlatformService storePlatformService;

    @GetMapping("/platform/list")
    @Operation(summary = "平台列表")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:query')")
    public CommonResult<List<XqPlatformRespVO>> listPlatforms() {
        return success(storePlatformService.listPlatforms());
    }

    @PostMapping("/platform/save")
    @Operation(summary = "保存平台")
    @PreAuthorize("@ss.hasAnyPermissions('xq:store-platform:create', 'xq:store-platform:update')")
    public CommonResult<Long> savePlatform(@Valid @RequestBody XqPlatformSaveReqVO reqVO) {
        return success(storePlatformService.savePlatform(reqVO));
    }

    @DeleteMapping("/platform/delete")
    @Operation(summary = "删除平台")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:delete')")
    public CommonResult<Boolean> deletePlatform(@RequestParam("id") Long id) {
        storePlatformService.deletePlatform(id);
        return success(true);
    }

    @GetMapping("/store/list")
    @Operation(summary = "店铺列表（含账号掩码）")
    @Parameter(name = "platformId", description = "平台ID")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:query')")
    public CommonResult<List<XqStoreRespVO>> listStores(
            @RequestParam(value = "platformId", required = false) Long platformId) {
        return success(storePlatformService.listStores(platformId));
    }

    @PostMapping("/store/save")
    @Operation(summary = "保存店铺（可改账号密码）")
    @PreAuthorize("@ss.hasAnyPermissions('xq:store-platform:create', 'xq:store-platform:update')")
    public CommonResult<Long> saveStore(@Valid @RequestBody XqStoreSaveReqVO reqVO) {
        return success(storePlatformService.saveStore(reqVO));
    }

    @DeleteMapping("/store/delete")
    @Operation(summary = "删除店铺")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:delete')")
    public CommonResult<Boolean> deleteStore(@RequestParam("id") Long id) {
        storePlatformService.deleteStore(id);
        return success(true);
    }

    @GetMapping("/alias/list")
    @Operation(summary = "平台别名映射列表")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:query')")
    public CommonResult<List<XqPlatformAliasRespVO>> listAliases() {
        return success(storePlatformService.listAliases());
    }

    @PostMapping("/alias/save")
    @Operation(summary = "保存别名映射")
    @PreAuthorize("@ss.hasAnyPermissions('xq:store-platform:create', 'xq:store-platform:update')")
    public CommonResult<Long> saveAlias(@Valid @RequestBody XqPlatformAliasSaveReqVO reqVO) {
        return success(storePlatformService.saveAlias(reqVO));
    }

    @DeleteMapping("/alias/delete")
    @Operation(summary = "删除别名映射")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:delete')")
    public CommonResult<Boolean> deleteAlias(@RequestParam("id") Long id) {
        storePlatformService.deleteAlias(id);
        return success(true);
    }

    @GetMapping("/user-store")
    @Operation(summary = "查询用户绑店")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:query')")
    public CommonResult<XqUserStoreRespVO> getUserStores(@RequestParam("userId") Long userId) {
        return success(storePlatformService.getUserStores(userId));
    }

    @PostMapping("/user-store/bind")
    @Operation(summary = "绑定用户店铺（空列表=不限店）")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:bind')")
    public CommonResult<Boolean> bindUserStores(@Valid @RequestBody XqUserStoreBindReqVO reqVO) {
        storePlatformService.bindUserStores(reqVO);
        return success(true);
    }

    @GetMapping("/warehouse/list")
    @Operation(summary = "发货仓库列表")
    @PreAuthorize("@ss.hasPermission('xq:store-warehouse:query')")
    public CommonResult<List<XqWarehouseRespVO>> listWarehouses() {
        return success(storePlatformService.listWarehouses());
    }

    @PostMapping("/warehouse/save")
    @Operation(summary = "保存发货仓库")
    @PreAuthorize("@ss.hasPermission('xq:store-warehouse:update')")
    public CommonResult<Long> saveWarehouse(@Valid @RequestBody XqWarehouseSaveReqVO reqVO) {
        return success(storePlatformService.saveWarehouse(reqVO));
    }

    @DeleteMapping("/warehouse/delete")
    @Operation(summary = "删除发货仓库")
    @PreAuthorize("@ss.hasPermission('xq:store-warehouse:update')")
    public CommonResult<Boolean> deleteWarehouse(@RequestParam("id") Long id) {
        storePlatformService.deleteWarehouse(id);
        return success(true);
    }

    @GetMapping("/store-warehouse/list")
    @Operation(summary = "店铺发货仓绑定列表")
    @Parameter(name = "storeId", description = "店铺ID")
    @PreAuthorize("@ss.hasPermission('xq:store-warehouse:query')")
    public CommonResult<List<XqStoreWarehouseRespVO>> listStoreWarehouses(
            @RequestParam(value = "storeId", required = false) Long storeId) {
        return success(storePlatformService.listStoreWarehouses(storeId));
    }

    @PostMapping("/store-warehouse/save")
    @Operation(summary = "保存店铺发货仓绑定")
    @PreAuthorize("@ss.hasPermission('xq:store-warehouse:update')")
    public CommonResult<Long> saveStoreWarehouse(@Valid @RequestBody XqStoreWarehouseSaveReqVO reqVO) {
        return success(storePlatformService.saveStoreWarehouse(reqVO));
    }

    @DeleteMapping("/store-warehouse/delete")
    @Operation(summary = "删除店铺发货仓绑定")
    @PreAuthorize("@ss.hasPermission('xq:store-warehouse:update')")
    public CommonResult<Boolean> deleteStoreWarehouse(@RequestParam("id") Long id) {
        storePlatformService.deleteStoreWarehouse(id);
        return success(true);
    }

    @PostMapping("/sync-legacy")
    @Operation(summary = "从旧库同步平台/店铺凭据/用户绑店权限")
    @PreAuthorize("@ss.hasPermission('xq:store-platform:sync')")
    public CommonResult<XqStoreSyncRespVO> syncLegacy() {
        return success(storePlatformService.syncFromLegacy());
    }

}
