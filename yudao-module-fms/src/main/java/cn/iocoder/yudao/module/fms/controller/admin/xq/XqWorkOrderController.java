package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.service.xq.XqWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 作业单")
@RestController
@RequestMapping("/xq/work-order")
@Validated
public class XqWorkOrderController {

    @Resource
    private XqWorkOrderService workOrderService;

    @GetMapping("/page")
    @Operation(summary = "作业单分页")
    @PreAuthorize("@ss.hasPermission('xq:work-order:query')")
    public CommonResult<PageResult<XqWorkOrderRespVO>> getWorkOrderPage(@Valid XqWorkOrderPageReqVO pageReqVO) {
        PageResult<XqWorkOrderDO> page = workOrderService.getWorkOrderPage(pageReqVO);
        return success(BeanUtils.toBean(page, XqWorkOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得作业单")
    @PreAuthorize("@ss.hasPermission('xq:work-order:query')")
    public CommonResult<XqWorkOrderRespVO> getWorkOrder(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workOrderService.getWorkOrder(id), XqWorkOrderRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "更新作业文案")
    @PreAuthorize("@ss.hasPermission('xq:work-order:update')")
    public CommonResult<Boolean> updateWorkOrder(@Valid @RequestBody XqWorkOrderUpdateReqVO updateReqVO) {
        workOrderService.updateWorkOrder(updateReqVO);
        return success(true);
    }

    @PostMapping("/complete")
    @Operation(summary = "完成入库（写入品库）")
    @PreAuthorize("@ss.hasPermission('xq:work-order:complete')")
    public CommonResult<Long> completeWorkOrder(@Valid @RequestBody XqWorkOrderCompleteReqVO completeReqVO) {
        return success(workOrderService.completeWorkOrder(completeReqVO));
    }

}
