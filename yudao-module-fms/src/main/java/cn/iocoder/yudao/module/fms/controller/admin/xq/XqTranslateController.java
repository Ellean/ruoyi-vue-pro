package cn.iocoder.yudao.module.fms.controller.admin.xq;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateCopyPreviewReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateCopyPreviewRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateTextReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateTextRespVO;
import cn.iocoder.yudao.module.fms.service.xq.XqAliyunMtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "产品 - 阿里云机器翻译")
@RestController
@RequestMapping("/xq/translate")
@Validated
public class XqTranslateController {

    @Resource
    private XqAliyunMtService aliyunMtService;

    @GetMapping("/status")
    @Operation(summary = "机器翻译配置状态（是否已填 AccessKey）")
    @PreAuthorize("@ss.hasAnyPermissions('xq:work-order:query', 'xq:work-order:update', 'xq:template-config:query')")
    public CommonResult<Map<String, Object>> status() {
        return success(aliyunMtService.status());
    }

    @PostMapping("/text")
    @Operation(summary = "翻译单段文本（专业版 title/description，或 general）")
    @PreAuthorize("@ss.hasAnyPermissions('xq:work-order:query', 'xq:work-order:update')")
    public CommonResult<XqTranslateTextRespVO> translateText(
            @Valid @RequestBody XqTranslateTextReqVO reqVO) {
        return success(aliyunMtService.translateText(reqVO));
    }

    @PostMapping("/copy-preview")
    @Operation(summary = "文案中文对照：标题/卖点/长描述只译不写回")
    @PreAuthorize("@ss.hasAnyPermissions('xq:work-order:query', 'xq:work-order:update')")
    public CommonResult<XqTranslateCopyPreviewRespVO> translateCopyPreview(
            @Valid @RequestBody XqTranslateCopyPreviewReqVO reqVO) {
        return success(aliyunMtService.translateCopyPreview(reqVO));
    }
}
