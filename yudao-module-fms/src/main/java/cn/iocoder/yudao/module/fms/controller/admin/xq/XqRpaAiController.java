package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiUploadImageReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqRpaAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * RPA AI：机器人长跑不依赖 30 分钟登录态。
 * 鉴权用 body.callbackToken（与 xq.rpa.callback-token 一致），与文案/生图回调相同。
 * 生图/上传替代旧 ERP Aixoras/OSS；长等待为过渡，后续迁 worker。
 */
@Tag(name = "产品 - RPA AI")
@RestController
@RequestMapping("/xq/rpa/ai")
@Validated
public class XqRpaAiController {

    @Resource
    private XqRpaAiService rpaAiService;

    @PostMapping("/generate-copy")
    @Operation(summary = "RPA：按文案规则生成标题/卖点/突出内容")
    @PermitAll
    public CommonResult<Map<String, Object>> generateCopy(@Valid @RequestBody XqRpaAiGenerateCopyReqVO reqVO) {
        return success(rpaAiService.generateCopy(reqVO));
    }

    @PostMapping("/vision-profile")
    @Operation(summary = "RPA：下发全部可识图 Chat API（profiles），供本机一图一聊轮询")
    @PermitAll
    public CommonResult<Map<String, Object>> visionProfile(@RequestBody XqRpaAiClassifyImagesReqVO reqVO) {
        return success(rpaAiService.getVisionProfile(reqVO == null ? null : reqVO.getCallbackToken()));
    }

    @PostMapping("/classify-images")
    @Operation(summary = "RPA：无图兜底分型（正式识图在机器人本机）")
    @PermitAll
    public CommonResult<List<Map<String, Object>>> classifyImages(
            @Valid @RequestBody XqRpaAiClassifyImagesReqVO reqVO) {
        return success(rpaAiService.classifyImages(reqVO));
    }

    @PostMapping("/generate-image-prompts")
    @Operation(summary = "RPA：按文案卖点生成图片提示词")
    @PermitAll
    public CommonResult<List<Map<String, Object>>> generateImagePrompts(
            @Valid @RequestBody XqRpaAiImagePromptsReqVO reqVO) {
        return success(rpaAiService.generateImagePrompts(reqVO));
    }

    @PostMapping("/generate-image")
    @Operation(summary = "RPA：生图/改图（替代旧 ERP /rpa/aixoras/image）")
    @PermitAll
    public CommonResult<Map<String, Object>> generateImage(@Valid @RequestBody XqRpaAiGenerateImageReqVO reqVO) {
        return success(rpaAiService.generateImage(reqVO));
    }

    @PostMapping("/upload-image")
    @Operation(summary = "RPA：上传成品图（替代旧 ERP OSS/Postimages）")
    @PermitAll
    public CommonResult<Map<String, Object>> uploadImage(@Valid @RequestBody XqRpaAiUploadImageReqVO reqVO) {
        return success(rpaAiService.uploadImage(reqVO));
    }

}
