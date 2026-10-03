package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.product.XqProductSaveReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - 品库产品")
@RestController
@RequestMapping("/xq/product")
@Validated
public class XqProductController {

    @Resource
    private XqProductService productService;

    @PostMapping("/create")
    @Operation(summary = "创建品库产品（本地演示表）")
    @PreAuthorize("@ss.hasPermission('xq:product:create')")
    public CommonResult<Long> createProduct(@Valid @RequestBody XqProductSaveReqVO createReqVO) {
        return success(productService.createProduct(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新品库产品（本地演示表）")
    @PreAuthorize("@ss.hasPermission('xq:product:update')")
    public CommonResult<Boolean> updateProduct(@Valid @RequestBody XqProductSaveReqVO updateReqVO) {
        productService.updateProduct(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除品库产品（本地演示表）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('xq:product:delete')")
    public CommonResult<Boolean> deleteProduct(@RequestParam("id") Long id) {
        productService.deleteProduct(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得品库产品（Giga 原库）")
    @PreAuthorize("@ss.hasPermission('xq:product:query')")
    public CommonResult<XqProductRespVO> getProduct(@RequestParam("id") String id) {
        return success(productService.getProduct(id));
    }

    @GetMapping("/page")
    @Operation(summary = "品库产品分页（Giga 原库）")
    @PreAuthorize("@ss.hasPermission('xq:product:query')")
    public CommonResult<PageResult<XqProductRespVO>> getProductPage(@Valid XqProductPageReqVO pageReqVO) {
        return success(productService.getProductPage(pageReqVO));
    }

}
