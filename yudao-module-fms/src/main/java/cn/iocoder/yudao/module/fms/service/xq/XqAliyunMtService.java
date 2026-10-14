package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateCopyPreviewReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateCopyPreviewRespVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateTextReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.translate.XqTranslateTextRespVO;

import java.util.Map;

/**
 * 阿里云机器翻译（专业版电商 Scene=title/description；general 走通用版）。
 */
public interface XqAliyunMtService {

    /** AccessKey 是否已配置且 enabled */
    boolean isConfigured();

    Map<String, Object> status();

    XqTranslateTextRespVO translateText(XqTranslateTextReqVO reqVO);

    /** 文案区中文对照：标题/卖点/长描述，不写回任务 */
    XqTranslateCopyPreviewRespVO translateCopyPreview(XqTranslateCopyPreviewReqVO reqVO);
}
