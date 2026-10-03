package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "RPA AI - 识图分型请求")
@Data
public class XqRpaAiClassifyImagesReqVO {

    private String callbackToken;
    private List<String> sourceImages;
    private Boolean mock;

}
