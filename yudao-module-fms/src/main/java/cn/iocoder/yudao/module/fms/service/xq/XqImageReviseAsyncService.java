package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReviseImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReviseImageRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderReviseImageTaskVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_RPA_AI_FAIL;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_WORK_ORDER_ACCESS_DENIED;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.XQ_WORK_ORDER_NOT_EXISTS;

/**
 * 单图改图异步任务：HTTP 立即返回 taskId，实际生图在有界线程池。
 * 边界：仍跑在主 API 进程（过渡，限并发 2）；长期应迁独立 worker。
 */
@Service
@Slf4j
public class XqImageReviseAsyncService {

    private static final int KEEP_MS = 2 * 60 * 60 * 1000;

    @Resource
    private XqWorkOrderService workOrderService;

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private ThreadPoolExecutor executor;

    @PostConstruct
    public void init() {
        executor = new ThreadPoolExecutor(
                1, 2, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(8),
                r -> {
                    Thread t = new Thread(r, "xq-image-revise");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public XqWorkOrderReviseImageTaskVO submit(XqWorkOrderReviseImageReqVO reqVO, Long userId) {
        purgeExpired();
        if (reqVO == null || reqVO.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        String taskId = "rev-" + IdUtil.fastSimpleUUID();
        Task task = new Task();
        task.taskId = taskId;
        task.userId = userId;
        task.workOrderId = reqVO.getId();
        task.promptIndex = reqVO.getPromptIndex();
        task.status = "pending";
        task.message = "已提交，等待上游改图…";
        task.createdAt = System.currentTimeMillis();
        tasks.put(taskId, task);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        try {
            executor.execute(() -> {
                SecurityContextHolder.setContext(securityContext);
                try {
                    run(task, reqVO, userId);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        } catch (Exception ex) {
            tasks.remove(taskId);
            throw exception(XQ_RPA_AI_FAIL, "改图排队已满，请稍后再试");
        }
        log.info("[revise-async] queued taskId={} wo={} idx={}", taskId, reqVO.getId(), reqVO.getPromptIndex());
        return toVo(task);
    }

    public XqWorkOrderReviseImageTaskVO get(String taskId, Long userId) {
        Task task = tasks.get(StrUtil.trim(taskId));
        if (task == null) {
            throw exception(XQ_RPA_AI_FAIL, "改图任务不存在或已过期");
        }
        if (userId == null || !Objects.equals(task.userId, userId)) {
            throw exception(XQ_WORK_ORDER_ACCESS_DENIED);
        }
        return toVo(task);
    }

    private void run(Task task, XqWorkOrderReviseImageReqVO reqVO, Long userId) {
        task.status = "running";
        task.message = "数字员工生成中，请稍候…";
        try {
            XqWorkOrderReviseImageRespVO result = workOrderService.reviseImagePreview(reqVO, userId);
            task.result = result;
            task.status = "success";
            task.message = "改图完成，请确认是否替换";
        } catch (ServiceException ex) {
            task.status = "fail";
            task.message = StrUtil.blankToDefault(ex.getMessage(), "改图失败");
            log.warn("[revise-async] fail taskId={} err={}", task.taskId, ex.getMessage());
        } catch (Exception ex) {
            task.status = "fail";
            task.message = StrUtil.blankToDefault(ex.getMessage(), "改图失败");
            log.warn("[revise-async] fail taskId={}", task.taskId, ex);
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        tasks.entrySet().removeIf(e -> now - e.getValue().createdAt > KEEP_MS);
    }

    private static XqWorkOrderReviseImageTaskVO toVo(Task task) {
        XqWorkOrderReviseImageTaskVO vo = new XqWorkOrderReviseImageTaskVO();
        vo.setTaskId(task.taskId);
        vo.setStatus(task.status);
        vo.setMessage(task.message);
        vo.setWorkOrderId(task.workOrderId);
        vo.setPromptIndex(task.promptIndex);
        vo.setResult(task.result);
        return vo;
    }

    @Data
    private static class Task {
        private String taskId;
        private Long userId;
        private Long workOrderId;
        private Integer promptIndex;
        private String status;
        private String message;
        private long createdAt;
        private XqWorkOrderReviseImageRespVO result;
    }

}
