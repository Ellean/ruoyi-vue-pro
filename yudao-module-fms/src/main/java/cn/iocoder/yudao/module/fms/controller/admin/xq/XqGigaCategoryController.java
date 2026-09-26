package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.category.XqGigaCategoryTreeRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaSiteCategoryDO;
import cn.iocoder.yudao.module.fms.service.xq.XqGigaCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - Giga 官网类目")
@RestController
@RequestMapping("/xq/category")
@Validated
public class XqGigaCategoryController {

    @Resource
    private XqGigaCategoryService categoryService;

    @GetMapping("/tree")
    @Operation(summary = "官网类目树")
    @PreAuthorize("@ss.hasPermission('xq:product:query')")
    public CommonResult<List<XqGigaCategoryTreeRespVO>> getTree() {
        return success(categoryService.getTree());
    }

    @GetMapping("/level1")
    @Operation(summary = "一级类目（顶栏）")
    @PreAuthorize("@ss.hasPermission('xq:product:query')")
    public CommonResult<List<XqGigaCategoryTreeRespVO>> getLevel1() {
        List<XqGigaSiteCategoryDO> list = categoryService.getLevel1();
        return success(convertList(list, row -> {
            XqGigaCategoryTreeRespVO vo = new XqGigaCategoryTreeRespVO();
            vo.setId(row.getGigaId());
            vo.setGigaId(row.getGigaId());
            vo.setName(row.getName());
            vo.setLevel(row.getLevel());
            vo.setImagePath(row.getImagePath());
            vo.setPathIds(row.getPathIds());
            vo.setPathNames(row.getPathNames());
            return vo;
        }));
    }

}
