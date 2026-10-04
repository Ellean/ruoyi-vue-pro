package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;

import java.util.List;
import java.util.Map;

/**
 * RPA 文案流水线 AI：主 API 只跑文案 Chat，并把模型地址交给机器人。
 * 原图下载与识图在 RPA 本机，不在服务端转二进制。
 */
public interface XqRpaAiService {

    Map<String, Object> generateCopy(XqRpaAiGenerateCopyReqVO reqVO);

    Map<String, Object> getVisionProfile(String callbackToken);

    List<Map<String, Object>> classifyImages(XqRpaAiClassifyImagesReqVO reqVO);

    List<Map<String, Object>> generateImagePrompts(XqRpaAiImagePromptsReqVO reqVO);

}
