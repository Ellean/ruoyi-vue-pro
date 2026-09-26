package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderAssignImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderBatchIdsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderCompleteReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderDispatchReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderPageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.service.xq.XqWorkOrderService;
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
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 工作台任务")
@RestController
@RequestMapping("/xq/work-order")
@Validated
public class XqWorkOrderController {

    @Resource
    private XqWorkOrderService workOrderService;

    @GetMapping("/page")
    @Operation(summary = "工作台任务分页")
    @PreAuthorize("@ss.hasPermission('xq:work-order:query')")
    public CommonResult<PageResult<XqWorkOrderRespVO>> getWorkOrderPage(@Valid XqWorkOrderPageReqVO pageReqVO) {
        PageResult<XqWorkOrderDO> page = workOrderService.getWorkOrderPage(pageReqVO);
        return success(BeanUtils.toBean(page, XqWorkOrderRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得工作台任务")
    @PreAuthorize("@ss.hasPermission('xq:work-order:query')")
    public CommonResult<XqWorkOrderRespVO> getWorkOrder(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workOrderService.getWorkOrder(id), XqWorkOrderRespVO.class));
    }

    @PostMapping("/dispatch")
    @Operation(summary = "选品库下发到工作台")
    @PreAuthorize("@ss.hasPermission('xq:product:dispatch')")
    public CommonResult<List<XqWorkOrderRespVO>> dispatchFromLibrary(
            @Valid @RequestBody XqWorkOrderDispatchReqVO reqVO) {
        List<XqWorkOrderDO> list = workOrderService.dispatchFromLibrary(reqVO, getLoginUserId());
        return success(BeanUtils.toBean(list, XqWorkOrderRespVO.class));
    }

    @PutMapping("/update")
    @Operation(summary = "更新文案")
    @PreAuthorize("@ss.hasPermission('xq:work-order:update')")
    public CommonResult<Boolean> updateWorkOrder(@Valid @RequestBody XqWorkOrderUpdateReqVO updateReqVO) {
        workOrderService.updateWorkOrder(updateReqVO);
        return success(true);
    }

    @PostMapping("/generate-copy")
    @Operation(summary = "生成文案（演示）")
    @Parameter(name = "id", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('xq:work-order:gen-copy')")
    public CommonResult<XqWorkOrderRespVO> generateCopy(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workOrderService.generateCopy(id, getLoginUserId()), XqWorkOrderRespVO.class));
    }

    @PostMapping("/batch-generate-copy")
    @Operation(summary = "批量生成文案并领取")
    @PreAuthorize("@ss.hasPermission('xq:work-order:batch-copy')")
    public CommonResult<List<XqWorkOrderRespVO>> batchGenerateCopy(
            @Valid @RequestBody XqWorkOrderBatchIdsReqVO reqVO) {
        List<XqWorkOrderDO> list = workOrderService.batchGenerateCopy(reqVO, getLoginUserId());
        return success(BeanUtils.toBean(list, XqWorkOrderRespVO.class));
    }

    @PostMapping("/batch-assign-image")
    @Operation(summary = "批量分配美工")
    @PreAuthorize("@ss.hasPermission('xq:work-order:assign-image')")
    public CommonResult<Integer> batchAssignImage(@Valid @RequestBody XqWorkOrderAssignImageReqVO reqVO) {
        return success(workOrderService.batchAssignImage(reqVO, getLoginUserId()));
    }

    @PostMapping("/generate-image")
    @Operation(summary = "生成图片（演示）")
    @Parameter(name = "id", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('xq:work-order:gen-image')")
    public CommonResult<XqWorkOrderRespVO> generateImage(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workOrderService.generateImage(id), XqWorkOrderRespVO.class));
    }

    @PostMapping("/complete")
    @Operation(summary = "上架完成（写入本地品库）")
    @PreAuthorize("@ss.hasPermission('xq:work-order:complete')")
    public CommonResult<Long> completeWorkOrder(@Valid @RequestBody XqWorkOrderCompleteReqVO completeReqVO) {
        return success(workOrderService.completeWorkOrder(completeReqVO));
    }

}
