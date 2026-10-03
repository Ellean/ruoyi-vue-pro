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
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaCopyCallbackReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaCopyPullReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaCopyPullRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderUpdateReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.service.xq.XqCopyPipelineService;
import cn.iocoder.yudao.module.fms.service.xq.XqWorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
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
    @Resource
    private XqCopyPipelineService copyPipelineService;

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
    @Operation(summary = "触发生成文案 RPA（规则→文案→图提示词）")
    @Parameter(name = "id", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('xq:work-order:gen-copy')")
    public CommonResult<XqWorkOrderRespVO> generateCopy(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(workOrderService.generateCopy(id, getLoginUserId()), XqWorkOrderRespVO.class));
    }

    @PostMapping("/batch-generate-copy")
    @Operation(summary = "批量触发文案 RPA 并领取")
    @PreAuthorize("@ss.hasPermission('xq:work-order:batch-copy')")
    public CommonResult<List<XqWorkOrderRespVO>> batchGenerateCopy(
            @Valid @RequestBody XqWorkOrderBatchIdsReqVO reqVO) {
        List<XqWorkOrderDO> list = workOrderService.batchGenerateCopy(reqVO, getLoginUserId());
        return success(BeanUtils.toBean(list, XqWorkOrderRespVO.class));
    }

    @PostMapping("/rpa-copy-pull")
    @Operation(summary = "RPA：按当前登录人领取待跑文案 SKU")
    @PreAuthorize("@ss.hasPermission('xq:work-order:query')")
    public CommonResult<XqWorkOrderRpaCopyPullRespVO> rpaCopyPull(
            @RequestBody(required = false) XqWorkOrderRpaCopyPullReqVO reqVO) {
        Integer limit = reqVO == null ? null : reqVO.getLimit();
        List<java.util.Map<String, Object>> jobs = copyPipelineService.pullCopyJobs(getLoginUserId(), limit);
        XqWorkOrderRpaCopyPullRespVO resp = new XqWorkOrderRpaCopyPullRespVO();
        resp.setJobs(jobs);
        resp.setCount(jobs.size());
        return success(resp);
    }

    @GetMapping("/rpa-copy-detail")
    @Operation(summary = "RPA：按 SKU 拉文案详情（原文案/原图/规则）")
    @Parameter(name = "sku", description = "Item Code / SKU", required = true)
    @PreAuthorize("@ss.hasPermission('xq:work-order:query')")
    public CommonResult<java.util.Map<String, Object>> rpaCopyDetail(@RequestParam("sku") String sku) {
        return success(copyPipelineService.buildCopyDetailBySku(getLoginUserId(), sku));
    }

    @PostMapping("/rpa-copy-callback")
    @Operation(summary = "文案 RPA 回调（写回文案与图片提示词）")
    @PermitAll
    public CommonResult<Boolean> rpaCopyCallback(@Valid @RequestBody XqWorkOrderRpaCopyCallbackReqVO reqVO) {
        copyPipelineService.handleCallback(reqVO);
        return success(true);
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

    @PostMapping("/close")
    @Operation(summary = "关闭进行中任务并清理进度（文案/美工/生成图）")
    @Parameter(name = "id", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('xq:work-order:close')")
    public CommonResult<Boolean> closeWorkOrder(@RequestParam("id") Long id) {
        workOrderService.closeWorkOrder(id);
        return success(true);
    }

    @PostMapping("/batch-close")
    @Operation(summary = "批量关闭并清理进度")
    @PreAuthorize("@ss.hasPermission('xq:work-order:close')")
    public CommonResult<Integer> batchCloseWorkOrder(@Valid @RequestBody XqWorkOrderBatchIdsReqVO reqVO) {
        return success(workOrderService.batchCloseWorkOrder(reqVO));
    }

}
