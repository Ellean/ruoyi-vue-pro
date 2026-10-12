package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaCopyCallbackReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaTriggerRespVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import cn.iocoder.yudao.module.fms.service.xq.rpa.CommanderRpaJobLauncher;
import cn.iocoder.yudao.module.fms.service.xq.rpa.XqRpaJobLauncherRegistry;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqRpaUserConfigMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.fms.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.fms.service.xq.XqSourceItemServiceImpl.WORK_STATUS_DOING;

/**
 * 跑在主 API：只组包/触发/落库；AI 识图与文案生成在「文案生成」RPA worker。
 */
@Service
@Validated
@Slf4j
public class XqCopyPipelineServiceImpl implements XqCopyPipelineService {

    public static final String RPA_STATUS_QUEUED = "queued";
    public static final String RPA_STATUS_RUNNING = "running";
    public static final String RPA_STATUS_SUCCESS = "success";
    public static final String RPA_STATUS_FAIL = "fail";

    @Value("${xq.rpa.callback-base-url:http://127.0.0.1:48080/admin-api}")
    private String callbackBaseUrl;
    @Value("${xq.rpa.callback-token:xq-rpa-callback}")
    private String callbackToken;

    @Resource
    private XqWorkOrderMapper workOrderMapper;
    @Resource
    private XqGigaProductMapper gigaProductMapper;
    @Resource
    private XqListingCatalogService listingCatalogService;
    @Resource
    private XqRpaUserConfigMapper rpaUserConfigMapper;
    @Resource
    private XqRpaConfigService rpaConfigService;
    @Lazy
    @Resource
    private XqCopyPipelineServiceImpl self;

    @Override
    public Map<String, Object> buildJobInput(XqWorkOrderDO order, Long userId) {
        enrichSourceIfNeeded(order);

        XqCopyGenRuleRespVO copyRule = listingCatalogService.getCopyRule(order.getListingPlatformId());
        XqImageGenRuleRespVO imageRule = listingCatalogService.getImageRule(
                order.getListingPlatformId(), order.getListingCategoryId());

        Map<String, Object> input = new LinkedHashMap<>();
        // 登录类入参（与个人 RPA 配置一致，流程可选用）
        // 表缺 runner_mode 等列时不能拖垮详情：先容错，再靠 SQL 补齐
        try {
            XqRpaUserConfigDO userCfg = rpaUserConfigMapper.selectByUserId(userId);
            if (userCfg != null) {
                if (StrUtil.isNotBlank(userCfg.getErpSiteUrl())) {
                    input.put("http", userCfg.getErpSiteUrl().trim());
                }
                if (StrUtil.isNotBlank(userCfg.getAccount())) {
                    input.put("账户", userCfg.getAccount().trim());
                }
                if (StrUtil.isNotBlank(userCfg.getPassword())) {
                    input.put("密码", userCfg.getPassword().trim());
                }
            }
        } catch (Exception ex) {
            log.warn("[buildJobInput] 读取个人RPA配置失败 userId={} err={}", userId, ex.getMessage());
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            String tid = String.valueOf(tenantId);
            input.put("租户", tid);
            input.put("tenantId", tid);
        }

        input.put("pipeline", "copy_then_image_prompt");
        input.put("workOrderId", order.getId());
        input.put("workOrderNo", order.getNo());
        input.put("sku", order.getExternalSku());
        input.put("title", order.getTitle());
        input.put("coverUrl", order.getCoverUrl());
        input.put("categoryName", order.getCategoryName());
        input.put("listingPlatformId", order.getListingPlatformId());
        input.put("listingPlatformName", order.getListingPlatformName());
        input.put("listingCategoryId", order.getListingCategoryId());
        input.put("listingCategoryName", order.getListingCategoryName());
        input.put("originalCopy", StrUtil.blankToDefault(order.getSourceDescription(), ""));
        JSONObject copyJson = parseCopyResultJson(order.getCopyResultJson());
        boolean regenerate = "revise".equalsIgnoreCase(copyJson.getStr("mode"))
                || StrUtil.isNotBlank(copyJson.getStr("revisionPrompt"));
        String revisionPrompt = StrUtil.blankToDefault(copyJson.getStr("revisionPrompt"), "");
        String target = StrUtil.blankToDefault(copyJson.getStr("target"), "all").toLowerCase();
        Integer featureIndex = copyJson.getInt("featureIndex");
        Map<String, Object> existingCopy = new LinkedHashMap<>();
        existingCopy.put("title", StrUtil.blankToDefault(order.getContentTitle(), ""));
        existingCopy.put("sellingPoints", parseSellingPointList(order.getContentSellingPoints()));
        existingCopy.put("description", StrUtil.blankToDefault(copyJson.getStr("description"), ""));
        input.put("regenerate", regenerate);
        input.put("revisionPrompt", revisionPrompt);
        input.put("existingCopy", existingCopy);
        input.put("target", target);
        if (featureIndex != null) {
            input.put("featureIndex", featureIndex);
        }
        // 批量导入的分项提示词：标题 / 卖点 / 长描述
        Object copyPrompts = copyJson.get("copyPrompts");
        if (copyPrompts != null) {
            input.put("copyPrompts", copyPrompts);
        }
        List<String> sourceImages = parseImageUrls(order.getSourceImageUrls(), order.getCoverUrl());
        // 详情/组包不再直接抛错：交给机器人下载步骤报「无图」；避免 HTTP 层只显示「系统异常」
        input.put("sourceImages", sourceImages);

        Map<String, Object> copyRulePayload = new LinkedHashMap<>();
        copyRulePayload.put("platformId", copyRule.getPlatformId());
        copyRulePayload.put("name", copyRule.getName());
        copyRulePayload.put("configJson", StrUtil.blankToDefault(copyRule.getConfigJson(), "{}"));
        input.put("copyRule", copyRulePayload);

        Map<String, Object> imageRulePayload = new LinkedHashMap<>();
        imageRulePayload.put("platformId", imageRule.getPlatformId());
        imageRulePayload.put("categoryId", imageRule.getCategoryId());
        imageRulePayload.put("name", imageRule.getName());
        imageRulePayload.put("promptText", StrUtil.blankToDefault(imageRule.getPromptText(), ""));
        imageRulePayload.put("negativePrompt", StrUtil.blankToDefault(imageRule.getNegativePrompt(), ""));
        imageRulePayload.put("configJson", StrUtil.blankToDefault(imageRule.getConfigJson(), "{}"));
        input.put("imageRule", imageRulePayload);

        // 流水线步骤说明（给 RPA / 大模型 system 用）
        input.put("steps", List.of(
                "1.load_copy_rule: 先加载文案规则作为硬约束（字数/卖点数/标点/平台规范）",
                "2.download_images: RPA 必须把 sourceImages 全部下到本机 Main Images（与首图识图无关）",
                "3.vision_observe: 本机只识首图 → imageObservations（不做全套识图）",
                "4.generate_copy: 按首图观察+原文案+copyPrompts分项提示词写标题/卖点/中文风格标签/长描述",
                "5.image_prompts_local: 提示词用文案本地拼，全套识图留给生图机器人"
        ));

        String base = StrUtil.removeSuffix(StrUtil.blankToDefault(callbackBaseUrl, ""), "/");
        input.put("apiBaseUrl", base);
        input.put("aiGenerateCopyPath", "/xq/rpa/ai/generate-copy");
        input.put("aiVisionProfilePath", "/xq/rpa/ai/vision-profile");
        input.put("aiClassifyImagesPath", "/xq/rpa/ai/classify-images");
        input.put("aiGenerateImagePromptsPath", "/xq/rpa/ai/generate-image-prompts");
        input.put("callbackUrl", base + "/xq/work-order/rpa-copy-callback");
        input.put("callbackToken", callbackToken);
        return input;
    }

    @Override
    public void enqueueCopyJob(XqWorkOrderDO order, Long userId) {
        if (order == null || order.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        // 扁平入队：主体与勾选变体都可入队，不再按父子过滤
        enrichSourceIfNeeded(order);
        if (parseImageUrls(order.getSourceImageUrls(), order.getCoverUrl()).isEmpty()) {
            throw exception(XQ_RPA_AI_FAIL, "任务没有参考图，无法跑文案");
        }
        workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getCopyUserId, userId)
                .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_QUEUED)
                .set(XqWorkOrderDO::getRpaCopyError, null)
                .set(XqWorkOrderDO::getRpaCopyWorkUuid, null)
                .set(XqWorkOrderDO::getWorkflowPhase, "copy")
                .set(XqWorkOrderDO::getAssigneeUserId,
                        order.getAssigneeUserId() == null ? userId : order.getAssigneeUserId()));
    }

    @Override
    public String triggerCopyJob(XqWorkOrderDO order, Long userId) {
        if (order == null || order.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        XqRpaUserConfigDO userCfg = rpaUserConfigMapper.selectByUserId(userId);
        if (userCfg == null) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        String mode = XqRpaJobLauncherRegistry.normalizeMode(userCfg.getRunnerMode());
        // 控制中枢需要 Job UUID；本地脚本/exe 用 runnerPath
        if (CommanderRpaJobLauncher.MODE.equals(mode) && StrUtil.isBlank(userCfg.getCopyJobUuid())) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        if (!CommanderRpaJobLauncher.MODE.equals(mode) && StrUtil.isBlank(userCfg.getRunnerPath())) {
            throw exception(XQ_RPA_TRIGGER_FAIL, "本地执行未配置脚本/exe 路径");
        }

        enrichSourceIfNeeded(order);
        Map<String, Object> input = buildJobInput(order, userId);
        try {
            String jobUuid = StrUtil.blankToDefault(userCfg.getCopyJobUuid(), userCfg.getRunnerPath());
            XqRpaTriggerRespVO result = rpaConfigService.launch(userId, "copy", jobUuid, input);
            String workUuid = result == null ? null : result.getWorkUuid();
            workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, order.getId())
                    .set(XqWorkOrderDO::getRpaCopyWorkUuid, workUuid)
                    .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_QUEUED)
                    .set(XqWorkOrderDO::getRpaCopyError, null)
                    .set(XqWorkOrderDO::getCopyUserId, userId)
                    .set(XqWorkOrderDO::getWorkflowPhase, "copy")
                    .set(XqWorkOrderDO::getAssigneeUserId,
                            order.getAssigneeUserId() == null ? userId : order.getAssigneeUserId()));
            return workUuid;
        } catch (Exception ex) {
            workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, order.getId())
                    .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_FAIL)
                    .set(XqWorkOrderDO::getRpaCopyError,
                            StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(), "触发失败"), 500)));
            throw exception(XQ_RPA_TRIGGER_FAIL, StrUtil.blankToDefault(ex.getMessage(), "未知错误"));
        }
    }

    @Override
    public List<XqWorkOrderDO> triggerBatch(List<Long> ids, Long userId) {
        if (CollUtil.isEmpty(ids)) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        List<XqWorkOrderDO> result = new ArrayList<>();
        for (Long id : ids) {
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null) {
                throw exception(XQ_WORK_ORDER_NOT_EXISTS);
            }
            triggerCopyJob(order, userId);
            result.add(workOrderMapper.selectById(id));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> pullCopyJobs(Long userId, Integer limit) {
        if (userId == null) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        // 未传 limit 时按扁平队列一次多拉（上限见 selectPendingCopyJobs=20），避免只领 1 条
        int n = limit == null || limit < 1 ? 20 : limit;
        List<XqWorkOrderDO> orders = workOrderMapper.selectPendingCopyJobs(userId, n);
        if (orders.isEmpty()) {
            long mine = workOrderMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getStatus, WORK_STATUS_DOING)
                    .eq(XqWorkOrderDO::getAssigneeUserId, userId)
                    .eq(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_QUEUED));
            long anyQueued = workOrderMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getStatus, WORK_STATUS_DOING)
                    .eq(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_QUEUED));
            log.warn("[pullCopyJobs] empty userId={} tenantId={} queuedForAssignee={} queuedAll={}",
                    userId, TenantContextHolder.getTenantId(), mine, anyQueued);
        }
        // 只返回精简 jobs（sku/workOrderId），详情由 rpa-copy-detail 再拉。
        // 禁止在拉取阶段因无图/组包失败 continue：否则 queuedForUser>0 但 jobs=[]，机器人侧会被误判为「没识别到字段」。
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (XqWorkOrderDO order : orders) {
            String sku = StrUtil.blankToDefault(order.getExternalSku(), "").trim();
            if (StrUtil.isBlank(sku)) {
                log.warn("[pullCopyJobs] skip blank-sku id={}", order.getId());
                continue;
            }
            workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, order.getId())
                    .set(XqWorkOrderDO::getCopyUserId, userId)
                    .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_RUNNING)
                    .set(XqWorkOrderDO::getRpaCopyError, null)
                    .set(XqWorkOrderDO::getWorkflowPhase, "copy")
                    .set(XqWorkOrderDO::getAssigneeUserId,
                            order.getAssigneeUserId() == null ? userId : order.getAssigneeUserId()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workOrderId", order.getId());
            row.put("sku", sku);
            row.put("title", order.getTitle());
            jobs.add(row);
        }
        log.info("[pullCopyJobs] userId={} tenantId={} matched={} returned={}",
                userId, TenantContextHolder.getTenantId(), orders.size(), jobs.size());
        return jobs;
    }

    @Override
    public List<Map<String, Object>> peekCopyJobs(Long userId, Integer limit) {
        if (userId == null) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        int n = limit == null || limit < 1 ? 20 : limit;
        List<XqWorkOrderDO> orders = workOrderMapper.selectPendingCopyJobs(userId, n);
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (XqWorkOrderDO order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workOrderId", order.getId());
            row.put("sku", order.getExternalSku());
            row.put("title", order.getTitle());
            row.put("copyUserId", order.getCopyUserId());
            row.put("rpaCopyStatus", order.getRpaCopyStatus());
            row.put("status", order.getStatus());
            jobs.add(row);
        }
        log.info("[peekCopyJobs] userId={} tenantId={} matched={}",
                userId, TenantContextHolder.getTenantId(), jobs.size());
        return jobs;
    }

    @Override
    public List<Map<String, Object>> peekDeskCopyJobs(Long userId, Integer limit) {
        if (userId == null) {
            return List.of();
        }
        int n = limit == null || limit < 1 ? 20 : Math.min(limit, 50);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (XqWorkOrderDO order : workOrderMapper.selectDoingByAssignee(userId, n)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workOrderId", order.getId());
            row.put("sku", order.getExternalSku());
            row.put("parentWorkOrderId", order.getParentWorkOrderId());
            row.put("assigneeUserId", order.getAssigneeUserId());
            row.put("copyUserId", order.getCopyUserId());
            row.put("rpaCopyStatus", order.getRpaCopyStatus());
            row.put("status", order.getStatus());
            rows.add(row);
        }
        log.info("[peekDeskCopyJobs] userId={} size={}", userId, rows.size());
        return rows;
    }

    @Override
    public Map<String, Object> buildCopyDetailBySku(Long userId, String sku) {
        if (userId == null || StrUtil.isBlank(sku)) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        XqWorkOrderDO order = workOrderMapper.selectDoingCopyBySku(userId, sku.trim());
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        enrichSourceIfNeeded(order);
        return buildJobInput(order, userId);
    }

    @Override
    public Map<String, Object> buildCopyDetailById(Long userId, Long workOrderId) {
        if (userId == null || workOrderId == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        XqWorkOrderDO order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        enrichSourceIfNeeded(order);
        return buildJobInput(order, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(XqWorkOrderRpaCopyCallbackReqVO reqVO) {
        if (reqVO == null || reqVO.getWorkOrderId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!StrUtil.equals(callbackToken, StrUtil.blankToDefault(reqVO.getCallbackToken(), ""))) {
            throw exception(XQ_RPA_TRIGGER_FAIL, "回调 token 无效");
        }
        XqWorkOrderDO order = workOrderMapper.selectById(reqVO.getWorkOrderId());
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }

        String status = StrUtil.blankToDefault(reqVO.getStatus(), "").trim().toLowerCase();
        if ("fail".equals(status) || "failed".equals(status) || "error".equals(status)) {
            workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, order.getId())
                    .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_FAIL)
                    .set(XqWorkOrderDO::getRpaCopyError,
                            StrUtil.maxLength(StrUtil.blankToDefault(reqVO.getErrorMessage(), "RPA 失败"), 500))
                    .set(XqWorkOrderDO::getRpaCopyWorkUuid,
                            StrUtil.blankToDefault(reqVO.getWorkUuid(), order.getRpaCopyWorkUuid())));
            return;
        }

        Map<String, Object> copyResult = reqVO.getCopyResult();
        if (copyResult == null) {
            copyResult = new LinkedHashMap<>();
        }
        String title = StrUtil.blankToDefault(reqVO.getTitle(), str(copyResult.get("title")));
        String description = StrUtil.blankToDefault(reqVO.getDescription(), str(copyResult.get("description")));
        String selling = normalizeSellingPoints(reqVO.getSellingPoints());
        if (StrUtil.isBlank(selling)) {
            selling = normalizeSellingPoints(copyResult.get("sellingPoints"));
        }
        if (StrUtil.isBlank(selling)) {
            selling = normalizeSellingPoints(copyResult.get("bulletPoints"));
        }
        if (StrUtil.isBlank(title)) {
            title = StrUtil.blankToDefault(order.getTitle(), order.getExternalSku());
        }
        copyResult.putIfAbsent("title", title);
        copyResult.putIfAbsent("description", description);
        copyResult.putIfAbsent("sellingPoints", selling);
        copyResult.remove("mode");
        copyResult.remove("revisionPrompt");
        copyResult.remove("target");
        copyResult.remove("featureIndex");
        // 保留工作台写入的分项提示词，避免回调覆盖后丢失
        if (!copyResult.containsKey("copyPrompts")) {
            JSONObject prev = parseCopyResultJson(order.getCopyResultJson());
            Object kept = prev.get("copyPrompts");
            if (kept != null) {
                copyResult.put("copyPrompts", kept);
            }
        }
        Object hs = copyResult.get("highlightStyle");
        if (hs instanceof String s && s.equals(description)) {
            copyResult.remove("highlightStyle");
        }

        String imagePromptJson = "[]";
        List<String> promptUrls = new ArrayList<>();
        if (CollUtil.isNotEmpty(reqVO.getImagePrompts())) {
            imagePromptJson = JSONUtil.toJsonStr(reqVO.getImagePrompts());
            for (Map<String, Object> item : reqVO.getImagePrompts()) {
                String url = str(item.get("imageUrl"));
                if (StrUtil.startWithAny(url, "http://", "https://")) {
                    promptUrls.add(url);
                }
            }
        }

        LambdaUpdateWrapper<XqWorkOrderDO> done = new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getContentTitle, title)
                .set(XqWorkOrderDO::getContentSellingPoints, selling)
                .set(XqWorkOrderDO::getCopyResultJson, JSONUtil.toJsonStr(copyResult))
                .set(XqWorkOrderDO::getImagePromptJson, imagePromptJson)
                .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_SUCCESS)
                .set(XqWorkOrderDO::getRpaCopyError, null)
                .set(XqWorkOrderDO::getRpaCopyWorkUuid,
                        StrUtil.blankToDefault(reqVO.getWorkUuid(), order.getRpaCopyWorkUuid()))
                .set(XqWorkOrderDO::getWorkflowPhase, "image");
        if (StrUtil.isBlank(order.getSourceImageUrls()) && !promptUrls.isEmpty()) {
            done.set(XqWorkOrderDO::getSourceImageUrls, JSONUtil.toJsonStr(promptUrls));
            if (StrUtil.isBlank(order.getCoverUrl())) {
                done.set(XqWorkOrderDO::getCoverUrl, promptUrls.get(0));
            }
        }
        int updated;
        try {
            updated = workOrderMapper.update(null, done);
        } catch (Exception ex) {
            // 常见：content_selling_points 过短（历史 varchar(1024)）导致整单回滚，前端一直「生成中」
            log.warn("[rpa-copy-callback] 落库异常 workOrderId={} sellingLen={} titleLen={} err={}",
                    order.getId(), StrUtil.length(selling), StrUtil.length(title), ex.getMessage());
            throw exception(XQ_RPA_TRIGGER_FAIL,
                    "回调落库失败：" + StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(), "db error"), 200));
        }
        if (updated <= 0) {
            log.warn("[rpa-copy-callback] 更新 0 行 workOrderId={} status={}",
                    order.getId(), order.getRpaCopyStatus());
            throw exception(XQ_RPA_TRIGGER_FAIL, "回调落库失败：工单未更新 id=" + order.getId());
        }
        log.info("[rpa-copy-callback] ok workOrderId={} sku={} titleLen={} sellingLen={}",
                order.getId(), order.getExternalSku(), StrUtil.length(title), StrUtil.length(selling));

        // 主体成功后：未勾选独立 RPA 的变体沿用主体文案
        if (order.getParentWorkOrderId() == null) {
            inheritCopyToUnselectedChildren(order.getId(), title, selling, JSONUtil.toJsonStr(copyResult));
        }
    }

    /** 未勾选变体默认使用主体跑出来的文案 */
    private void inheritCopyToUnselectedChildren(Long rootId, String title, String selling, String copyResultJson) {
        if (rootId == null) {
            return;
        }
        List<XqWorkOrderDO> children = workOrderMapper.selectByParentIds(List.of(rootId));
        for (XqWorkOrderDO child : children) {
            if (child == null || child.getId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(child.getCopyRpaSelected())) {
                continue;
            }
            if ("queued".equals(child.getRpaCopyStatus()) || "running".equals(child.getRpaCopyStatus())) {
                continue;
            }
            workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, child.getId())
                    .set(XqWorkOrderDO::getContentTitle, title)
                    .set(XqWorkOrderDO::getContentSellingPoints, selling)
                    .set(XqWorkOrderDO::getCopyResultJson, copyResultJson)
                    .set(XqWorkOrderDO::getRpaCopyStatus, "inherited")
                    .set(XqWorkOrderDO::getRpaCopyError, null)
                    .set(XqWorkOrderDO::getWorkflowPhase, "image"));
        }
    }

    /**
     * 下发后补齐原文案/原图；生成前再兜底一次。
     * <p>
     * 原图只取「本 SKU」在 Giga 的图集，禁止合并父工单/家族其它变体图
     * （否则会出现粉/白等其它颜色混进当前变体原图区）。
     * 若本 SKU 有完整图集而工单里被历史逻辑污染，会纠正回本 SKU。
     */
    public void enrichSourceIfNeeded(XqWorkOrderDO order) {
        if (order == null || order.getId() == null) {
            return;
        }
        boolean needDesc = StrUtil.isBlank(order.getSourceDescription());
        List<String> currentImages = parseImageUrls(order.getSourceImageUrls(), order.getCoverUrl());
        XqGigaProductRow row = self.readGigaProduct(order.getGigaProductId(), order.getExternalSku());

        List<String> selfImages = row == null
                ? List.of()
                : parseImageUrls(row.getImageUrlsJson(), row.getImageUrl());
        // 仅本 SKU：有图则以其为准；无图则不拉父/家族，避免串变体
        boolean needImages = !selfImages.isEmpty() && !sameUrlList(currentImages, selfImages);
        if (row == null && !needDesc && !needImages) {
            return;
        }
        if (!needDesc && !needImages
                && (row == null || !(StrUtil.isBlank(order.getGigaProductId())
                && StrUtil.isNotBlank(row.getId())))) {
            return;
        }
        LambdaUpdateWrapper<XqWorkOrderDO> uw = new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId());
        boolean changed = false;
        if (needDesc && row != null && StrUtil.isNotBlank(row.getDescription())) {
            uw.set(XqWorkOrderDO::getSourceDescription, row.getDescription());
            order.setSourceDescription(row.getDescription());
            changed = true;
        }
        if (needImages) {
            List<String> urls = new ArrayList<>(selfImages);
            String json = JSONUtil.toJsonStr(urls);
            uw.set(XqWorkOrderDO::getSourceImageUrls, json);
            order.setSourceImageUrls(json);
            // 封面优先用本 SKU 主图
            String selfCover = StrUtil.blankToDefault(row.getImageUrl(), urls.get(0));
            if (StrUtil.isNotBlank(selfCover)
                    && !StrUtil.equals(selfCover, order.getCoverUrl())) {
                uw.set(XqWorkOrderDO::getCoverUrl, selfCover);
                order.setCoverUrl(selfCover);
            } else if (StrUtil.isBlank(order.getCoverUrl())) {
                uw.set(XqWorkOrderDO::getCoverUrl, urls.get(0));
                order.setCoverUrl(urls.get(0));
            }
            changed = true;
        }
        if (row != null && StrUtil.isBlank(order.getGigaProductId()) && StrUtil.isNotBlank(row.getId())) {
            uw.set(XqWorkOrderDO::getGigaProductId, row.getId());
            order.setGigaProductId(row.getId());
            changed = true;
        }
        if (changed) {
            workOrderMapper.update(null, uw);
        }
    }

    private static boolean sameUrlList(List<String> a, List<String> b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!StrUtil.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public XqGigaProductRow readGigaProduct(String gigaProductId, String sku) {
        try {
            if (StrUtil.isNotBlank(gigaProductId)) {
                XqGigaProductRow row = gigaProductMapper.selectById(gigaProductId);
                if (row != null) {
                    return row;
                }
            }
            if (StrUtil.isNotBlank(sku)) {
                return gigaProductMapper.selectBySku(sku);
            }
        } catch (Exception ex) {
            log.warn("[readGigaProduct] 读取 Giga 产品失败 id={} sku={}: {}",
                    gigaProductId, sku, ex.getMessage());
        }
        return null;
    }

    private static List<String> parseImageUrls(String json, String cover) {
        Set<String> set = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(cover)) {
            String c = cover.trim();
            if (c.startsWith("//")) {
                c = "https:" + c;
            }
            if (c.startsWith("http://") || c.startsWith("https://")) {
                set.add(c);
            }
        }
        if (StrUtil.isNotBlank(json) && !"null".equalsIgnoreCase(json)) {
            try {
                if (json.trim().startsWith("[")) {
                    JSONArray arr = JSONUtil.parseArray(json);
                    for (Object o : arr) {
                        String url;
                        if (o instanceof JSONObject jo) {
                            url = StrUtil.blankToDefault(jo.getStr("url"),
                                    StrUtil.blankToDefault(jo.getStr("imageUrl"), jo.getStr("src")));
                        } else {
                            url = StrUtil.trim(String.valueOf(o));
                        }
                        if (StrUtil.isNotBlank(url) && !"null".equalsIgnoreCase(url)) {
                            if (url.startsWith("//")) {
                                url = "https:" + url;
                            }
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                set.add(url);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return new ArrayList<>(set);
    }

    private static String normalizeSellingPoints(Object raw) {
        if (raw == null) {
            return "";
        }
        if (raw instanceof List<?> list) {
            List<String> lines = new ArrayList<>();
            for (Object o : list) {
                String s = StrUtil.trim(String.valueOf(o));
                if (StrUtil.isNotBlank(s)) {
                    lines.add(s.startsWith("•") || s.startsWith("-") ? s : ("• " + s));
                }
            }
            return String.join("\n", lines);
        }
        String text = String.valueOf(raw).trim();
        if (StrUtil.isBlank(text)) {
            return "";
        }
        if (text.startsWith("[")) {
            try {
                return normalizeSellingPoints(JSONUtil.parseArray(text));
            } catch (Exception ignored) {
                // fallthrough
            }
        }
        return text;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static JSONObject parseCopyResultJson(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(raw);
        } catch (Exception ex) {
            return new JSONObject();
        }
    }

    private static List<String> parseSellingPointList(String raw) {
        if (StrUtil.isBlank(raw)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : raw.split("\\r?\\n+")) {
            String s = StrUtil.trim(line.replaceFirst("^\\s*[•\\-*]\\s*", ""));
            if (StrUtil.isNotBlank(s)) {
                out.add(s);
            }
        }
        return out;
    }

}
