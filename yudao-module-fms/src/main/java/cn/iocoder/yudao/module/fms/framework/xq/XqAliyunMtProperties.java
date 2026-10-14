package cn.iocoder.yudao.module.fms.framework.xq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云机器翻译（专业版电商引擎）配置。
 * 开通后填写 access-key-id / access-key-secret；未填时接口返回友好提示。
 */
@Component
@ConfigurationProperties(prefix = "xq.aliyun.mt")
@Data
public class XqAliyunMtProperties {

    /** 是否启用（仍需配置 AK） */
    private boolean enabled = true;

    /** 地域 Endpoint，默认杭州 */
    private String endpoint = "mt.cn-hangzhou.aliyuncs.com";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    /** 默认源语言，auto 自动识别 */
    private String defaultSourceLanguage = "auto";

    /** 默认目标语言 */
    private String defaultTargetLanguage = "zh";

    /** 连接/读超时 */
    private int connectTimeoutMs = 8000;

    private int readTimeoutMs = 20000;
}
