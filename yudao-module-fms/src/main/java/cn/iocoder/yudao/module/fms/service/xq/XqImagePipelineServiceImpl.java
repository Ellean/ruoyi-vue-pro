package cn.iocoder.yudao.module.fms.service.xq;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.listing.XqImageGenRuleRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.workorder.XqWorkOrderRpaImageCallbackReqVO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqRpaUserConfigDO;
import cn.iocoder.yudao.module.fms.dal.dataobject.xq.XqWorkOrderDO;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqRpaUserConfigMapper;
import cn.iocoder.yudao.module.fms.dal.mysql.xq.XqWorkOrderMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
 * 跑在主 API：只组包/入队/落库；AI 生图在「生图」RPA worker。
 */
@Service
@Validated
@Slf4j
public class XqImagePipelineServiceImpl implements XqImagePipelineService {

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
    private XqListingCatalogService listingCatalogService;
    @Resource
    private XqRpaUserConfigMapper rpaUserConfigMapper;

    @Override
    public Map<String, Object> buildJobInput(XqWorkOrderDO order, Long userId) {
        if (order == null || order.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        XqImageGenRuleRespVO imageRule = listingCatalogService.getImageRule(
                order.getListingPlatformId(), order.getListingCategoryId());

        Map<String, Object> input = new LinkedHashMap<>();
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

        input.put("pipeline", "image_generate");
        input.put("workOrderId", order.getId());
        input.put("workOrderNo", order.getNo());
        input.put("sku", order.getExternalSku());
        input.put("title", StrUtil.blankToDefault(order.getContentTitle(), order.getTitle()));
        input.put("coverUrl", order.getCoverUrl());
        input.put("categoryName", order.getCategoryName());
        input.put("listingPlatformId", order.getListingPlatformId());
        input.put("listingPlatformName", order.getListingPlatformName());
        input.put("listingCategoryId", order.getListingCategoryId());
        input.put("listingCategoryName", order.getListingCategoryName());

        List<String> sourceImages = parseImageUrls(order.getSourceImageUrls(), order.getCoverUrl());
        if (sourceImages.isEmpty()) {
            throw exception(XQ_RPA_AI_FAIL, "任务没有参考图，无法生图");
        }
        input.put("sourceImages", sourceImages);

        List<Map<String, Object>> prompts = parseImagePrompts(order.getImagePromptJson());
        if (prompts.isEmpty()) {
            throw exception(XQ_RPA_AI_FAIL, "请先填写并保存图片提示词");
        }
        boolean hasText = prompts.stream().anyMatch(p -> StrUtil.isNotBlank(str(p.get("promptText"))));
        if (!hasText) {
            throw exception(XQ_RPA_AI_FAIL, "请先填写并保存图片提示词");
        }
        input.put("imagePrompts", prompts);
        input.put("highlightStyle", parseHighlightStyle(order));

        Map<String, Object> imageRulePayload = new LinkedHashMap<>();
        imageRulePayload.put("platformId", imageRule.getPlatformId());
        imageRulePayload.put("categoryId", imageRule.getCategoryId());
        imageRulePayload.put("name", imageRule.getName());
        imageRulePayload.put("promptText", StrUtil.blankToDefault(imageRule.getPromptText(), ""));
        imageRulePayload.put("negativePrompt", StrUtil.blankToDefault(imageRule.getNegativePrompt(), ""));
        imageRulePayload.put("configJson", StrUtil.blankToDefault(imageRule.getConfigJson(), "{}"));
        input.put("imageRule", imageRulePayload);

        input.put("steps", List.of(
                "1.load_prompts: 读取 imagePrompts（含 bindIndexes / promptText）",
                "2.download_bound_images: 按绑定原图下载到本机",
                "3.generate_images: 调用生图模型按槽位出图",
                "4.upload_results: 上传成品并写回 resultUrl",
                "5.callback: 回调 rpa-image-callback"
        ));

        String base = StrUtil.removeSuffix(StrUtil.blankToDefault(callbackBaseUrl, ""), "/");
        input.put("apiBaseUrl", base);
        input.put("callbackUrl", base + "/xq/work-order/rpa-image-callback");
        input.put("callbackToken", callbackToken);
        return input;
    }

    @Override
    public void enqueueImageJob(XqWorkOrderDO order, Long userId) {
        if (order == null || order.getId() == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        if (!Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
            throw exception(XQ_WORK_ORDER_STATUS_INVALID);
        }
        // 校验提示词与原图
        buildJobInput(order, userId);

        Long imageUserId = order.getImageUserId() != null ? order.getImageUserId() : userId;
        workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getImageUserId, imageUserId)
                .set(XqWorkOrderDO::getRpaImageStatus, RPA_STATUS_QUEUED)
                .set(XqWorkOrderDO::getRpaImageError, null)
                .set(XqWorkOrderDO::getRpaImageWorkUuid, null)
                .set(XqWorkOrderDO::getImageStatus, "running")
                .set(XqWorkOrderDO::getWorkflowPhase, "image")
                .set(XqWorkOrderDO::getAssigneeUserId,
                        order.getAssigneeUserId() == null ? userId : order.getAssigneeUserId()));
    }

    @Override
    public int enqueueImageJobs(List<Long> ids, Long userId) {
        if (CollUtil.isEmpty(ids)) {
            throw exception(XQ_DISPATCH_EMPTY);
        }
        int n = 0;
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            XqWorkOrderDO order = workOrderMapper.selectById(id);
            if (order == null || !Integer.valueOf(WORK_STATUS_DOING).equals(order.getStatus())) {
                continue;
            }
            enqueueImageJob(order, userId);
            n += 1;
        }
        return n;
    }

    @Override
    public List<Map<String, Object>> pullImageJobs(Long userId, Integer limit) {
        if (userId == null) {
            throw exception(XQ_RPA_CONFIG_INVALID);
        }
        int n = limit == null || limit < 1 ? 10 : limit;
        List<XqWorkOrderDO> orders = workOrderMapper.selectPendingImageJobs(userId, n);
        List<Map<String, Object>> jobs = new ArrayList<>();
        for (XqWorkOrderDO order : orders) {
            workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                    .eq(XqWorkOrderDO::getId, order.getId())
                    .set(XqWorkOrderDO::getImageUserId, userId)
                    .set(XqWorkOrderDO::getRpaImageStatus, RPA_STATUS_RUNNING)
                    .set(XqWorkOrderDO::getRpaImageError, null)
                    .set(XqWorkOrderDO::getImageStatus, "running")
                    .set(XqWorkOrderDO::getWorkflowPhase, "image"));
            try {
                jobs.add(buildJobInput(order, userId));
            } catch (Exception ex) {
                workOrderMapper.update(null, new LambdaUpdateWrapper<XqWorkOrderDO>()
                        .eq(XqWorkOrderDO::getId, order.getId())
                        .set(XqWorkOrderDO::getRpaImageStatus, RPA_STATUS_FAIL)
                        .set(XqWorkOrderDO::getRpaImageError,
                                StrUtil.maxLength(StrUtil.blankToDefault(ex.getMessage(), "组包失败"), 500))
                        .set(XqWorkOrderDO::getImageStatus, "todo"));
            }
        }
        return jobs;
    }

    @Override
    public Map<String, Object> buildImageDetailById(Long userId, Long workOrderId) {
        if (userId == null || workOrderId == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        XqWorkOrderDO order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw exception(XQ_WORK_ORDER_NOT_EXISTS);
        }
        return buildJobInput(order, userId);
    }

    @Override
    public void handleCallback(XqWorkOrderRpaImageCallbackReqVO reqVO) {
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
                    .set(XqWorkOrderDO::getRpaImageStatus, RPA_STATUS_FAIL)
                    .set(XqWorkOrderDO::getRpaImageError,
                            StrUtil.maxLength(StrUtil.blankToDefault(reqVO.getErrorMessage(), "生图 RPA 失败"), 500))
                    .set(XqWorkOrderDO::getRpaImageWorkUuid,
                            StrUtil.blankToDefault(reqVO.getWorkUuid(), order.getRpaImageWorkUuid()))
                    .set(XqWorkOrderDO::getImageStatus, "todo"));
            return;
        }

        List<Map<String, Object>> existing = parseImagePrompts(order.getImagePromptJson());
        List<Map<String, Object>> incoming = reqVO.getImagePrompts();
        List<Map<String, Object>> merged = mergeImagePrompts(existing, incoming);
        String cover = StrUtil.blankToDefault(reqVO.getGeneratedImageUrl(), "");
        if (StrUtil.isBlank(cover)) {
            for (Map<String, Object> p : merged) {
                String url = firstUrl(p.get("resultUrl"), p.get("generatedImageUrl"));
                if (StrUtil.isNotBlank(url)) {
                    cover = url;
                    break;
                }
            }
        }

        LambdaUpdateWrapper<XqWorkOrderDO> done = new LambdaUpdateWrapper<XqWorkOrderDO>()
                .eq(XqWorkOrderDO::getId, order.getId())
                .set(XqWorkOrderDO::getImagePromptJson, JSONUtil.toJsonStr(merged))
                .set(XqWorkOrderDO::getRpaImageStatus, RPA_STATUS_SUCCESS)
                .set(XqWorkOrderDO::getRpaImageError, null)
                .set(XqWorkOrderDO::getRpaImageWorkUuid,
                        StrUtil.blankToDefault(reqVO.getWorkUuid(), order.getRpaImageWorkUuid()))
                .set(XqWorkOrderDO::getImageStatus, "generated")
                .set(XqWorkOrderDO::getWorkflowPhase, "image");
        if (StrUtil.isNotBlank(cover)) {
            done.set(XqWorkOrderDO::getGeneratedImageUrl, cover);
        }
        int updated = workOrderMapper.update(null, done);
        if (updated <= 0) {
            throw exception(XQ_RPA_TRIGGER_FAIL, "回调落库失败：工单未更新 id=" + order.getId());
        }
        log.info("[rpa-image-callback] ok workOrderId={} sku={} prompts={}",
                order.getId(), order.getExternalSku(), merged.size());
    }

    private static List<Map<String, Object>> mergeImagePrompts(
            List<Map<String, Object>> existing, List<Map<String, Object>> incoming) {
        if (CollUtil.isEmpty(incoming)) {
            return existing;
        }
        if (CollUtil.isEmpty(existing)) {
            return incoming;
        }
        Map<Integer, Map<String, Object>> byIndex = new LinkedHashMap<>();
        for (int i = 0; i < existing.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>(existing.get(i));
            int idx = toIndex(row.get("index"), i);
            row.put("index", idx);
            byIndex.put(idx, row);
        }
        for (int i = 0; i < incoming.size(); i++) {
            Map<String, Object> src = incoming.get(i);
            if (src == null) {
                continue;
            }
            int idx = toIndex(src.get("index"), i);
            Map<String, Object> row = byIndex.getOrDefault(idx, new LinkedHashMap<>());
            row.put("index", idx);
            if (src.get("imageType") != null) {
                row.put("imageType", src.get("imageType"));
            }
            if (src.get("promptText") != null) {
                row.put("promptText", src.get("promptText"));
            }
            if (src.get("bindIndexes") != null) {
                row.put("bindIndexes", src.get("bindIndexes"));
            }
            if (src.get("imageUrl") != null) {
                row.put("imageUrl", src.get("imageUrl"));
            }
            String result = firstUrl(src.get("resultUrl"), src.get("generatedImageUrl"), src.get("url"));
            if (StrUtil.isNotBlank(result)) {
                row.put("resultUrl", result);
                row.put("generatedImageUrl", result);
                row.put("marker", StrUtil.blankToDefault(str(src.get("marker")), "ai"));
            }
            byIndex.put(idx, row);
        }
        return new ArrayList<>(byIndex.values());
    }

    private static int toIndex(Object raw, int fallback) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private List<Object> parseHighlightStyle(XqWorkOrderDO order) {
        List<Object> tags = new ArrayList<>();
        // 突出风格落在 copy_result_json.highlightStyle（无独立 contentHighlight 列）
        if (StrUtil.isBlank(order.getCopyResultJson())) {
            return tags;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(order.getCopyResultJson());
            Object hs = obj.get("highlightStyle");
            if (hs instanceof JSONArray arr) {
                for (Object o : arr) {
                    String s = StrUtil.trim(String.valueOf(o));
                    if (StrUtil.isNotBlank(s)) {
                        tags.add(s);
                    }
                }
            } else if (hs instanceof String s && StrUtil.isNotBlank(s)) {
                for (String part : s.split("[,，、;；|/]+")) {
                    String t = StrUtil.trim(part);
                    if (StrUtil.isNotBlank(t)) {
                        tags.add(t);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return tags;
    }

    private static List<Map<String, Object>> parseImagePrompts(String json) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (StrUtil.isBlank(json)) {
            return out;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(json);
            for (int i = 0; i < arr.size(); i++) {
                Object item = arr.get(i);
                if (!(item instanceof JSONObject obj)) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("index", obj.getInt("index", i));
                row.put("imageType", obj.getStr("imageType"));
                row.put("promptText", obj.getStr("promptText"));
                row.put("imageUrl", obj.getStr("imageUrl"));
                row.put("resultUrl", obj.getStr("resultUrl"));
                row.put("generatedImageUrl", obj.getStr("generatedImageUrl"));
                row.put("marker", obj.getStr("marker"));
                Object binds = obj.get("bindIndexes");
                if (binds != null) {
                    row.put("bindIndexes", binds);
                }
                out.add(row);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return out;
    }

    private static List<String> parseImageUrls(String json, String coverUrl) {
        Set<String> set = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(coverUrl)) {
            set.add(coverUrl.trim());
        }
        if (StrUtil.isNotBlank(json)) {
            try {
                JSONArray arr = JSONUtil.parseArray(json);
                for (Object o : arr) {
                    String url = StrUtil.trim(String.valueOf(o));
                    if (StrUtil.isNotBlank(url) && !"null".equalsIgnoreCase(url)) {
                        set.add(url);
                    }
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        return new ArrayList<>(set);
    }

    private static String firstUrl(Object... vals) {
        for (Object v : vals) {
            String s = str(v);
            if (StrUtil.startWithAny(s, "http://", "https://")) {
                return s;
            }
        }
        return "";
    }

    private static String str(Object v) {
        return v == null ? "" : StrUtil.trim(String.valueOf(v));
    }

}
