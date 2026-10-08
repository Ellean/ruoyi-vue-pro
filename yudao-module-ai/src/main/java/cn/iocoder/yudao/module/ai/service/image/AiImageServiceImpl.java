package cn.iocoder.yudao.module.ai.service.image;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.ai.controller.admin.image.vo.AiImageDrawReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.image.vo.AiImagePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.image.vo.AiImagePublicPageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.image.vo.AiImageUpdateReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.image.vo.midjourney.AiMidjourneyActionReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.image.vo.midjourney.AiMidjourneyImagineReqVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.image.AiImageDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.mysql.image.AiImageMapper;
import cn.iocoder.yudao.module.ai.enums.image.AiImageStatusEnum;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.AiImageGatewayModelUtils;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.openai.OpenAiCompatibleImageEditApi;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.siliconflow.SiliconFlowImageOptions;
import cn.iocoder.yudao.module.ai.service.model.AiApiKeyService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.*;

/**
 * AI 绘画 Service 实现类
 *
 * @author fansili
 */
@Service
@Slf4j
public class AiImageServiceImpl implements AiImageService {

    @Resource
    private AiModelService modelService;

    @Resource
    private AiApiKeyService apiKeyService;

    @Resource
    private AiImageMapper imageMapper;

    @Resource
    private FileApi fileApi;

    @Override
    public PageResult<AiImageDO> getImagePageMy(Long userId, AiImagePageReqVO pageReqVO) {
        return imageMapper.selectPageMy(userId, pageReqVO);
    }

    @Override
    public PageResult<AiImageDO> getImagePagePublic(AiImagePublicPageReqVO pageReqVO) {
        return imageMapper.selectPage(pageReqVO);
    }

    @Override
    public AiImageDO getImage(Long id) {
        return imageMapper.selectById(id);
    }

    @Override
    public List<AiImageDO> getImageList(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return imageMapper.selectByIds(ids);
    }

    @Override
    public Long drawImage(Long userId, AiImageDrawReqVO drawReqVO) {
        // 1. 校验模型
        AiModelDO model = modelService.validateModel(drawReqVO.getModelId());

        // 参考图写入 options，便于详情回显 / 重新生成
        String referImageUrl = resolveReferImageUrl(drawReqVO);
        Map<String, String> options = drawReqVO.getOptions() != null
                ? new HashMap<>(drawReqVO.getOptions()) : new HashMap<>();
        if (StrUtil.isNotBlank(referImageUrl)) {
            options.put("referImageUrl", referImageUrl);
            drawReqVO.setReferImageUrl(referImageUrl);
        }
        drawReqVO.setOptions(options);

        // 2. 保存数据库
        Map<String, Object> optionMap = new HashMap<>(options);
        AiImageDO image = BeanUtils.toBean(drawReqVO, AiImageDO.class).setUserId(userId)
                .setPlatform(model.getPlatform()).setModelId(model.getId()).setModel(model.getModel())
                .setPublicStatus(false).setStatus(AiImageStatusEnum.IN_PROGRESS.getStatus())
                .setOptions(optionMap);
        imageMapper.insert(image);

        // 3. 异步绘制，后续前端通过返回的 id 进行轮询结果
        getSelf().executeDrawImage(image, drawReqVO, model);
        return image.getId();
    }

    @Async
    @SuppressWarnings("ConstantValue")
    public void executeDrawImage(AiImageDO image, AiImageDrawReqVO reqVO, AiModelDO model) {
        try {
            AiApiKeyDO apiKey = apiKeyService.validateApiKey(model.getKeyId());
            String referImageUrl = resolveReferImageUrl(reqVO);

            List<String> candidates;
            String seedModel = model.getModel();
            if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.OPENAI.getPlatform())
                    && StrUtil.isNotBlank(referImageUrl)
                    && StrUtil.isNotBlank(apiKey.getImageEditModel())) {
                seedModel = apiKey.getImageEditModel();
            }
            if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.OPENAI.getPlatform())
                    && AiImageGatewayModelUtils.isGptImageModel(seedModel)) {
                candidates = AiImageGatewayModelUtils.channelFailoverCandidates(apiKey, seedModel);
            } else {
                candidates = List.of(StrUtil.blankToDefault(seedModel, ""));
            }

            String usedModel = model.getModel();
            byte[] fileContent;
            Exception lastError = null;

            // 有参考图且 OpenAI：走改图（edits / reference_images），不走文生图
            if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.OPENAI.getPlatform())
                    && StrUtil.isNotBlank(referImageUrl)) {
                byte[] referBytes = null;
                if (!OpenAiCompatibleImageEditApi.isToapis(apiKey)) {
                    referBytes = HttpUtil.downloadBytes(referImageUrl);
                }
                OpenAiCompatibleImageEditApi.EditResult editResult = null;
                for (String candidate : candidates) {
                    if (StrUtil.isBlank(candidate)) {
                        continue;
                    }
                    try {
                        editResult = OpenAiCompatibleImageEditApi.edit(
                                apiKey, candidate, reqVO.getPrompt(),
                                reqVO.getWidth(), reqVO.getHeight(),
                                referImageUrl, referBytes);
                        usedModel = editResult.getModel();
                        if (!StrUtil.equals(candidate, model.getModel())) {
                            log.info("[executeDrawImage][image({}) 改图模型切换 {} -> {}]",
                                    image.getId(), model.getModel(), usedModel);
                        }
                        break;
                    } catch (Exception ex) {
                        lastError = ex;
                        if (AiImageGatewayModelUtils.isNoAvailableChannel(ex)) {
                            log.warn("[executeDrawImage][image({}) 改图无通道 model={}，尝试下一候选] {}",
                                    image.getId(), candidate, ex.getMessage());
                            continue;
                        }
                        throw ex;
                    }
                }
                if (editResult == null) {
                    throw lastError != null ? lastError
                            : new IllegalArgumentException("改图结果为空");
                }
                if (StrUtil.isNotEmpty(editResult.getB64Json())) {
                    fileContent = Base64.decode(editResult.getB64Json());
                } else if (StrUtil.isNotBlank(editResult.getUrl())) {
                    fileContent = HttpUtil.downloadBytes(editResult.getUrl());
                } else {
                    throw new IllegalArgumentException("改图结果缺少图片数据（url 与 b64_json 均为空）");
                }
            } else {
                ImageModel imageModel = modelService.getImageModel(model.getId());
                ImageResponse response = null;
                for (String candidate : candidates) {
                    if (StrUtil.isBlank(candidate)) {
                        continue;
                    }
                    try {
                        ImageOptions request = buildImageOptions(reqVO, model, candidate);
                        response = imageModel.call(new ImagePrompt(reqVO.getPrompt(), request));
                        if (response.getResult() == null) {
                            throw new IllegalArgumentException("生成结果为空");
                        }
                        usedModel = candidate;
                        if (!StrUtil.equals(candidate, model.getModel())) {
                            log.info("[executeDrawImage][image({}) 模型切换 {} -> {}]",
                                    image.getId(), model.getModel(), candidate);
                        }
                        break;
                    } catch (Exception ex) {
                        lastError = ex;
                        if (AiImageGatewayModelUtils.isNoAvailableChannel(ex)) {
                            log.warn("[executeDrawImage][image({}) 无通道 model={}，尝试下一候选] {}",
                                    image.getId(), candidate, ex.getMessage());
                            continue;
                        }
                        throw ex;
                    }
                }
                if (response == null || response.getResult() == null) {
                    throw lastError != null ? lastError
                            : new IllegalArgumentException("生成结果为空");
                }

                // 上传到文件服务（中转可能返回空 url + b64_json）
                var output = response.getResult().getOutput();
                String b64Json = output.getB64Json();
                String url = output.getUrl();
                if (StrUtil.isNotEmpty(b64Json)) {
                    fileContent = Base64.decode(b64Json);
                } else if (StrUtil.isNotBlank(url)) {
                    fileContent = HttpUtil.downloadBytes(url);
                } else {
                    throw new IllegalArgumentException("生成结果缺少图片数据（url 与 b64_json 均为空）");
                }
            }

            String filePath = fileApi.createFile(fileContent);

            // 更新数据库（记下实际打到上游的模型名）
            imageMapper.updateById(new AiImageDO().setId(image.getId())
                    .setStatus(AiImageStatusEnum.SUCCESS.getStatus())
                    .setModel(usedModel)
                    .setPicUrl(filePath).setFinishTime(LocalDateTime.now()));
        } catch (Exception ex) {
            log.error("[executeDrawImage][image({}) 生成异常]", image, ex);
            String tip = StrUtil.blankToDefault(ex.getMessage(), "生成失败");
            if (AiImageGatewayModelUtils.isNoAvailableChannel(ex)) {
                tip = tip + "。请到「AI 大模型 → API 密钥」核对中转类型/Base URL，"
                        + "并在中转控制台开通 gpt-image 通道；Hao 站模型标识需带 openai/ 前缀。";
            }
            imageMapper.updateById(new AiImageDO().setId(image.getId())
                    .setStatus(AiImageStatusEnum.FAIL.getStatus())
                    .setErrorMessage(StrUtil.maxLength(tip, 500))
                    .setFinishTime(LocalDateTime.now()));
        }
    }

    private static String resolveReferImageUrl(AiImageDrawReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        if (StrUtil.isNotBlank(reqVO.getReferImageUrl())) {
            return StrUtil.trim(reqVO.getReferImageUrl());
        }
        return StrUtil.trim(MapUtil.getStr(reqVO.getOptions(), "referImageUrl"));
    }

    private static ImageOptions buildImageOptions(AiImageDrawReqVO draw, AiModelDO model, String modelName) {
        String resolvedModel = StrUtil.blankToDefault(modelName, model.getModel());
        if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.OPENAI.getPlatform())) {
            // https://platform.openai.com/docs/api-reference/images/create
            // gpt-image-*：中转/官方都不接受 response_format，且默认返回 b64_json；勿传 style
            // dall-e-2/3：可显式要 b64_json；仅 dall-e-3 支持 style
            boolean gptImage = AiImageGatewayModelUtils.isGptImageModel(resolvedModel);
            OpenAiImageOptions.Builder builder = OpenAiImageOptions.builder()
                    .model(resolvedModel)
                    .height(draw.getHeight()).width(draw.getWidth());
            if (gptImage) {
                // Aixoras/中转：gpt-image 带 response_format 会拒；quality=auto 为常用兼容写法
                builder.quality("auto");
            } else {
                builder.responseFormat("b64_json");
            }
            String style = MapUtil.getStr(draw.getOptions(), "style");
            String bare = AiImageGatewayModelUtils.bareModel(resolvedModel);
            if (StrUtil.isNotEmpty(style) && StrUtil.equals(bare, "dall-e-3")) {
                builder.style(style);
            }
            return builder.build();
        } else if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.SILICON_FLOW.getPlatform())) {
            // https://docs.siliconflow.cn/cn/api-reference/images/images-generations
            return SiliconFlowImageOptions.builder().model(resolvedModel)
                    .height(draw.getHeight()).width(draw.getWidth())
                    .build();
        }  else if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.STABLE_DIFFUSION.getPlatform())) {
            // https://platform.stability.ai/docs/api-reference#tag/SDXL-and-SD1.6/operation/textToImage
            // https://platform.stability.ai/docs/api-reference#tag/Text-to-Image/operation/textToImage
            return StabilityAiImageOptions.builder().model(resolvedModel)
                    .height(draw.getHeight()).width(draw.getWidth())
                    .seed(Long.valueOf(draw.getOptions().get("seed")))
                    .cfgScale(Float.valueOf(draw.getOptions().get("scale")))
                    .steps(Integer.valueOf(draw.getOptions().get("steps")))
                    .sampler(String.valueOf(draw.getOptions().get("sampler")))
                    .stylePreset(String.valueOf(draw.getOptions().get("stylePreset")))
                    .clipGuidancePreset(String.valueOf(draw.getOptions().get("clipGuidancePreset")))
                    .build();
        } else if (ObjUtil.equal(model.getPlatform(), AiPlatformEnum.TONG_YI.getPlatform())) {
            return DashScopeImageOptions.builder()
                    .model(resolvedModel).n(1)
                    .height(draw.getHeight()).width(draw.getWidth())
                    .build();
        }
        throw new IllegalArgumentException("不支持的 AI 平台：" + model.getPlatform());
    }

    @Override
    public void deleteImageMy(Long id, Long userId) {
        // 1. 校验是否存在
        AiImageDO image = validateImageExists(id);
        if (ObjUtil.notEqual(image.getUserId(), userId)) {
            throw exception(IMAGE_NOT_EXISTS);
        }
        // 2. 删除记录
        imageMapper.deleteById(id);
    }

    @Override
    public PageResult<AiImageDO> getImagePage(AiImagePageReqVO pageReqVO) {
        return imageMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateImage(AiImageUpdateReqVO updateReqVO) {
        // 1. 校验存在
        validateImageExists(updateReqVO.getId());
        // 2. 更新发布状态
        imageMapper.updateById(BeanUtils.toBean(updateReqVO, AiImageDO.class));
    }

    @Override
    public void deleteImage(Long id) {
        // 1. 校验存在
        validateImageExists(id);
        // 2. 删除
        imageMapper.deleteById(id);
    }

    private AiImageDO validateImageExists(Long id) {
        AiImageDO image = imageMapper.selectById(id);
        if (image == null) {
            throw exception(IMAGE_NOT_EXISTS);
        }
        return image;
    }

    // ================ midjourney 专属 ================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long midjourneyImagine(Long userId, AiMidjourneyImagineReqVO drawReqVO) {
        // 1. 校验模型
        AiModelDO model = modelService.validateModel(drawReqVO.getModelId());
        Assert.equals(model.getPlatform(), AiPlatformEnum.MIDJOURNEY.getPlatform(), "平台不匹配");
        MidjourneyApi midjourneyApi = modelService.getMidjourneyApi(model.getId());

        // 2. 保存数据库
        AiImageDO image = BeanUtils.toBean(drawReqVO, AiImageDO.class).setUserId(userId).setPublicStatus(false)
                .setStatus(AiImageStatusEnum.IN_PROGRESS.getStatus())
                .setPlatform(AiPlatformEnum.MIDJOURNEY.getPlatform()).setModelId(model.getId()).setModel(model.getName());
        imageMapper.insert(image);

        // 3. 调用 Midjourney Proxy 提交任务
        List<String> base64Array = StrUtil.isBlank(drawReqVO.getReferImageUrl()) ? null :
                Collections.singletonList("data:image/jpeg;base64,".concat(Base64.encode(HttpUtil.downloadBytes(drawReqVO.getReferImageUrl()))));
        MidjourneyApi.ImagineRequest imagineRequest = new MidjourneyApi.ImagineRequest(
                base64Array, drawReqVO.getPrompt(),null,
                MidjourneyApi.ImagineRequest.buildState(drawReqVO.getWidth(),
                        drawReqVO.getHeight(), drawReqVO.getVersion(), model.getModel()));
        MidjourneyApi.SubmitResponse imagineResponse = midjourneyApi.imagine(imagineRequest);

        // 4.1 情况一【失败】：抛出业务异常
        if (!MidjourneyApi.SubmitCodeEnum.SUCCESS_CODES.contains(imagineResponse.code())) {
            String description = imagineResponse.description().contains("quota_not_enough") ?
                    "账户余额不足" : imagineResponse.description();
            throw exception(IMAGE_MIDJOURNEY_SUBMIT_FAIL, description);
        }

        // 4.2 情况二【成功】：更新 taskId 和参数
        imageMapper.updateById(new AiImageDO().setId(image.getId())
                .setTaskId(imagineResponse.result()).setOptions(BeanUtil.beanToMap(drawReqVO)));
        return image.getId();
    }

    @Override
    public Integer midjourneySync() {
        // 1.1 获取 Midjourney 平台，状态在 “进行中” 的 image
        List<AiImageDO> images = imageMapper.selectListByStatusAndPlatform(
                AiImageStatusEnum.IN_PROGRESS.getStatus(), AiPlatformEnum.MIDJOURNEY.getPlatform());
        if (CollUtil.isEmpty(images)) {
            return 0;
        }
        // 1.2 调用 Midjourney Proxy 获取任务进展
        MidjourneyApi midjourneyApi = modelService.getMidjourneyApi(images.get(0).getModelId());
        List<MidjourneyApi.Notify> taskList = midjourneyApi.getTaskList(convertSet(images, AiImageDO::getTaskId));
        Map<String, MidjourneyApi.Notify> taskMap = convertMap(taskList, MidjourneyApi.Notify::id);

        // 2. 逐个处理，更新进展
        int count = 0;
        for (AiImageDO image : images) {
            MidjourneyApi.Notify notify = taskMap.get(image.getTaskId());
            if (notify == null) {
                log.error("[midjourneySync][image({}) 查询不到进展]", image);
                continue;
            }
            count++;
            updateMidjourneyStatus(image, notify);
        }
        return count;
    }

    @Override
    public void midjourneyNotify(MidjourneyApi.Notify notify) {
        // 1. 校验 image 存在
        AiImageDO image = imageMapper.selectByTaskId(notify.id());
        if (image == null) {
            log.warn("[midjourneyNotify][回调任务({}) 不存在]", notify.id());
            return;
        }
        // 2. 更新状态
        updateMidjourneyStatus(image, notify);
    }

    private void updateMidjourneyStatus(AiImageDO image, MidjourneyApi.Notify notify) {
        // 1. 转换状态
        Integer status = null;
        LocalDateTime finishTime = null;
        if (StrUtil.isNotBlank(notify.status())) {
            MidjourneyApi.TaskStatusEnum taskStatusEnum = MidjourneyApi.TaskStatusEnum.valueOf(notify.status());
            if (MidjourneyApi.TaskStatusEnum.SUCCESS == taskStatusEnum) {
                status = AiImageStatusEnum.SUCCESS.getStatus();
                finishTime = LocalDateTime.now();
            } else if (MidjourneyApi.TaskStatusEnum.FAILURE == taskStatusEnum) {
                status = AiImageStatusEnum.FAIL.getStatus();
                finishTime = LocalDateTime.now();
            }
        }

        // 2. 上传图片
        String picUrl = null;
        if (StrUtil.isNotBlank(notify.imageUrl())) {
            try {
                picUrl = fileApi.createFile(HttpUtil.downloadBytes(notify.imageUrl()));
            } catch (Exception e) {
                picUrl = notify.imageUrl();
                log.warn("[updateMidjourneyStatus][图片({}) 地址({}) 上传失败]", image.getId(), notify.imageUrl(), e);
            }
        }

        // 3. 更新 image 状态
        imageMapper.updateById(new AiImageDO().setId(image.getId()).setStatus(status)
                .setPicUrl(picUrl).setButtons(notify.buttons()).setErrorMessage(notify.failReason())
                .setFinishTime(finishTime));
    }

    @Override
    public Long midjourneyAction(Long userId, AiMidjourneyActionReqVO reqVO) {
        // 1.1 检查 image
        AiImageDO image = validateImageExists(reqVO.getId());
        if (ObjUtil.notEqual(userId, image.getUserId())) {
            throw exception(IMAGE_NOT_EXISTS);
        }
        MidjourneyApi midjourneyApi = modelService.getMidjourneyApi(image.getModelId());
        // 1.2 检查 customId
        MidjourneyApi.Button button = CollUtil.findOne(image.getButtons(),
                buttonX -> buttonX.customId().equals(reqVO.getCustomId()));
        if (button == null) {
            throw exception(IMAGE_CUSTOM_ID_NOT_EXISTS);
        }

        // 2. 调用 Midjourney Proxy 提交任务
        MidjourneyApi.SubmitResponse actionResponse = midjourneyApi.action(
                new MidjourneyApi.ActionRequest(button.customId(), image.getTaskId(), null));
        if (!MidjourneyApi.SubmitCodeEnum.SUCCESS_CODES.contains(actionResponse.code())) {
            String description = actionResponse.description().contains("quota_not_enough") ?
                    "账户余额不足" : actionResponse.description();
            throw exception(IMAGE_MIDJOURNEY_SUBMIT_FAIL, description);
        }

        // 3. 新增 image 记录
        AiImageDO newImage = new AiImageDO().setUserId(image.getUserId()).setPublicStatus(false).setPrompt(image.getPrompt())
                .setStatus(AiImageStatusEnum.IN_PROGRESS.getStatus())
                .setPlatform(AiPlatformEnum.MIDJOURNEY.getPlatform())
                .setModel(image.getModel()).setWidth(image.getWidth()).setHeight(image.getHeight())
                .setOptions(image.getOptions()).setTaskId(actionResponse.result());
        imageMapper.insert(newImage);
        return newImage.getId();
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private AiImageServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

}
