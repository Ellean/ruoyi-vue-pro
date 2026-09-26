package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source.XqSourceItemPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.source.XqSourceItemRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqSourceItemDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.service.xq.XqSourceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 货源池")
@RestController
@RequestMapping("/xq/source")
@Validated
public class XqSourceItemController {

    @Resource
    private XqSourceItemService sourceItemService;

    @GetMapping("/page")
    @Operation(summary = "货源分页")
    @PreAuthorize("@ss.hasPermission('xq:source:query')")
    public CommonResult<PageResult<XqSourceItemRespVO>> getSourcePage(@Valid XqSourceItemPageReqVO pageReqVO) {
        PageResult<XqSourceItemDO> page = sourceItemService.getSourcePage(pageReqVO);
        return success(BeanUtils.toBean(page, XqSourceItemRespVO.class));
    }

    @PostMapping("/claim")
    @Operation(summary = "认领货源（生成作业单）")
    @Parameter(name = "id", description = "货源编号", required = true)
    @PreAuthorize("@ss.hasPermission('xq:source:claim')")
    public CommonResult<XqWorkOrderRespVO> claimSource(@RequestParam("id") Long id) {
        XqWorkOrderDO order = sourceItemService.claimSource(id, getLoginUserId());
        return success(BeanUtils.toBean(order, XqWorkOrderRespVO.class));
    }

}
