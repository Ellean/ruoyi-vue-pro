package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "测试页 - 本地 AI 图结果")
@Data
public class XqAgentAiTestRespVO {

    private String id;
    private String sku;
    private String productName;
    private Integer imageCount;
    private String coverUrl;
    private String batchNo;
    private String generatedAt;
    private LocalDateTime updateDate;
    private List<ImageItem> images = new ArrayList<>();

    @Data
    public static class ImageItem {
        private String name;
        private String url;
        private String displayUrl;
        private String ossUrl;
    }

}
