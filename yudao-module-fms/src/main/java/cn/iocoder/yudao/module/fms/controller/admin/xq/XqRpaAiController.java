package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;
import cn.iocoder.yudao.module.fms.service.xq.XqRpaAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
 * 文案 RPA AI：需后台登录（Authorization Bearer），callbackToken 仍校验。
 * 边界：主 API 提供 Chat 能力；RPA 编排规则→文案→识图→图提示词并回调落库。
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
    public CommonResult<Map<String, Object>> generateCopy(@Valid @RequestBody XqRpaAiGenerateCopyReqVO reqVO) {
        return success(rpaAiService.generateCopy(reqVO));
    }

    @PostMapping("/classify-images")
    @Operation(summary = "RPA：原图识图分型")
    public CommonResult<List<Map<String, Object>>> classifyImages(
            @Valid @RequestBody XqRpaAiClassifyImagesReqVO reqVO) {
        return success(rpaAiService.classifyImages(reqVO));
    }

    @PostMapping("/generate-image-prompts")
    @Operation(summary = "RPA：按文案卖点生成图片提示词")
    public CommonResult<List<Map<String, Object>>> generateImagePrompts(
            @Valid @RequestBody XqRpaAiImagePromptsReqVO reqVO) {
        return success(rpaAiService.generateImagePrompts(reqVO));
    }

}
