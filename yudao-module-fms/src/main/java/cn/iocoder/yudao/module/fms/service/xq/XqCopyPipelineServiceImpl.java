package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqCopyGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaCopyCallbackReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqGigaProductRow;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaGlobalConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqGigaProductMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqRpaUserConfigMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private XqRpaGlobalConfigService rpaGlobalConfigService;
    @Resource
    private XqCommanderClient commanderClient;

    @Override
    public Map<String, Object> buildJobInput(XqWorkOrderDO order, Long userId) {
        enrichSourceIfNeeded(order);

        XqCopyGenRuleRespVO copyRule = listingCatalogService.getCopyRule(order.getListingPlatformId());
        XqImageGenRuleRespVO imageRule = listingCatalogService.getImageRule(
                order.getListingPlatformId(), order.getListingCategoryId());

        Map<String, Object> input = new LinkedHashMap<>();
        // 登录类入参（与个人 RPA 配置一致，流程可选用）
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
        input.put("sourceImages", parseImageUrls(order.getSourceImageUrls(), order.getCoverUrl()));

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
                "2.generate_copy: 结合原文案+原图，优化标题/卖点/突出内容/长描述",
                "3.classify_images: 识别每张图类型（主图/尺寸图/细节图/场景图/包装图等）并标记",
                "4.generate_image_prompts: 按生成文案的卖点+突出内容+图类型写提示词；尺寸图须绑定产品原图+标注图"
        ));

        String base = StrUtil.removeSuffix(StrUtil.blankToDefault(callbackBaseUrl, ""), "/");
        input.put("apiBaseUrl", base);
        input.put("aiGenerateCopyPath", "/xq/rpa/ai/generate-copy");
        input.put("aiClassifyImagesPath", "/xq/rpa/ai/classify-images");
        input.put("aiGenerateImagePromptsPath", "/xq/rpa/ai/generate-image-prompts");
        input.put("callbackUrl", base + "/xq/work-order/rpa-copy-callback");
        input.put("callbackToken", callbackToken);
        return input;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String triggerCopyJob(XqWorkOrderDO order, Long userId) {
        if (order == null || order.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        XqRpaGlobalConfigDO global = rpaGlobalConfigService.requireConfigured();
        XqRpaUserConfigDO userCfg = rpaUserConfigMapper.selectByUserId(userId);
        if (userCfg == null || StrUtil.isBlank(userCfg.getCopyJobUuid())) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }

        enrichSourceIfNeeded(order);
        Map<String, Object> input = buildJobInput(order, userId);
        try {
            Map<String, Object> result = commanderClient.triggerJob(
                    global.getBaseUrl(), global.getAppKey(), global.getAppSecret(),
                    userCfg.getCopyJobUuid().trim(), input);
            String workUuid = (String) result.get("workUuid");
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> pullCopyJobs(Long userId, Integer limit) {
        if (userId == null) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        int n = limit == null ? 10 : limit;
        List<XqWorkOrderDO> orders = workOrderMapper.selectPendingCopyJobs(userId, n);
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (XqWorkOrderDO order : orders) {
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
            row.put("sku", order.getExternalSku());
            row.put("title", order.getTitle());
            jobs.add(row);
        }
        return jobs;
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
        if (!copyResult.containsKey("highlightStyle") && StrUtil.isNotBlank(description)) {
            copyResult.put("highlightStyle", description);
        }

        String imagePromptJson = "[]";
        if (CollUtil.isNotEmpty(reqVO.getImagePrompts())) {
            imagePromptJson = JSONUtil.toJsonStr(reqVO.getImagePrompts());
        }

        workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getContentTitle, title)
                .set(XqWorkOrderDO::getContentSellingPoints, selling)
                .set(XqWorkOrderDO::getCopyResultJson, JSONUtil.toJsonStr(copyResult))
                .set(XqWorkOrderDO::getImagePromptJson, imagePromptJson)
                .set(XqWorkOrderDO::getRpaCopyStatus, RPA_STATUS_SUCCESS)
                .set(XqWorkOrderDO::getRpaCopyError, null)
                .set(XqWorkOrderDO::getRpaCopyWorkUuid,
                        StrUtil.blankToDefault(reqVO.getWorkUuid(), order.getRpaCopyWorkUuid()))
                .set(XqWorkOrderDO::getWorkflowPhase, "image"));
    }

    /** 下发后补齐原文案/原图；生成前再兜底一次 */
    public void enrichSourceIfNeeded(XqWorkOrderDO order) {
        if (order == null || order.getId() == null) {
            return;
        }
        boolean needDesc = StrUtil.isBlank(order.getSourceDescription());
        boolean needImages = StrUtil.isBlank(order.getSourceImageUrls());
        if (!needDesc && !needImages) {
            return;
        }
        XqGigaProductRow row = null;
        if (StrUtil.isNotBlank(order.getGigaProductId())) {
            row = gigaProductMapper.selectById(order.getGigaProductId());
        }
        if (row == null && StrUtil.isNotBlank(order.getExternalSku())) {
            row = gigaProductMapper.selectBySku(order.getExternalSku());
        }
        if (row == null) {
            return;
        }
        LambdaUpdateWrapper<XqWorkOrderDO> uw = new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId());
        boolean changed = false;
        if (needDesc && StrUtil.isNotBlank(row.getDescription())) {
            uw.set(XqWorkOrderDO::getSourceDescription, row.getDescription());
            order.setSourceDescription(row.getDescription());
            changed = true;
        }
        if (needImages) {
            List<String> urls = parseImageUrls(row.getImageUrlsJson(), row.getImageUrl());
            if (!urls.isEmpty()) {
                String json = JSONUtil.toJsonStr(urls);
                uw.set(XqWorkOrderDO::getSourceImageUrls, json);
                order.setSourceImageUrls(json);
                if (StrUtil.isBlank(order.getCoverUrl())) {
                    uw.set(XqWorkOrderDO::getCoverUrl, urls.get(0));
                    order.setCoverUrl(urls.get(0));
                }
                changed = true;
            }
        }
        if (StrUtil.isBlank(order.getGigaProductId()) && StrUtil.isNotBlank(row.getId())) {
            uw.set(XqWorkOrderDO::getGigaProductId, row.getId());
            order.setGigaProductId(row.getId());
            changed = true;
        }
        if (changed) {
            workOrderMapper.update(null, uw);
        }
    }

    private static List<String> parseImageUrls(String json, String cover) {
        Set<String> set = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(cover)) {
            set.add(cover.trim());
        }
        if (StrUtil.isNotBlank(json) && !"null".equalsIgnoreCase(json)) {
            try {
                if (json.trim().startsWith("[")) {
                    JSONArray arr = JSONUtil.parseArray(json);
                    for (Object o : arr) {
                        String url = StrUtil.trim(String.valueOf(o));
                        if (StrUtil.isNotBlank(url) && !"null".equalsIgnoreCase(url)) {
                            set.add(url);
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

}
