package cn.iocoder.yudao.module.fms.service.xq;

import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiClassifyImagesReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateCopyReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiGenerateImageReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiImagePromptsReqVO;
import cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa.XqRpaAiUploadImageReqVO;

import java.util.List;
import java.util.Map;

/**
 * RPA AI：文案 / 识图配置 / 生图 / 上传。
 * 边界：生图同步等待上游（过渡）；长期应迁独立 worker。
 */
public interface XqRpaAiService {

    Map<String, Object> generateCopy(XqRpaAiGenerateCopyReqVO reqVO);

    Map<String, Object> getVisionProfile(String callbackToken);

    List<Map<String, Object>> classifyImages(XqRpaAiClassifyImagesReqVO reqVO);

    List<Map<String, Object>> generateImagePrompts(XqRpaAiImagePromptsReqVO reqVO);

    /** 生图/改图（对齐旧 Aixoras RPA 入参，回 url）；校验 callbackToken */
    Map<String, Object> generateImage(XqRpaAiGenerateImageReqVO reqVO);

    /**
     * 管理端登录态生图/改图（不校验 callbackToken）。
     * 边界：仍同步等待上游（过渡）；单图改图预览用，长期应迁 worker。
     */
    Map<String, Object> generateImageForConsole(XqRpaAiGenerateImageReqVO reqVO);

    /** 上传成品图，返回可公网访问 url */
    Map<String, Object> uploadImage(XqRpaAiUploadImageReqVO reqVO);

}
