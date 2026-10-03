package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;

import java.util.List;
import java.util.Map;

/**
 * RPA 文案流水线 AI 能力（跑在主 API，供文案 RPA 编排调用）。
 * 长耗时视觉任务后续可迁独立 worker；当前由 RPA 分步调用并限并发。
 */
public interface XqRpaAiService {

    Map<String, Object> generateCopy(XqRpaAiGenerateCopyReqVO reqVO);

    List<Map<String, Object>> classifyImages(XqRpaAiClassifyImagesReqVO reqVO);

    List<Map<String, Object>> generateImagePrompts(XqRpaAiImagePromptsReqVO reqVO);

}
