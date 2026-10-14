package cn.iocoder.yudao.module.fms.framework.xq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ToAPIs 用量查询（账号级访问令牌，非 sk- Key）。
 * 文档：https://docs.toapis.cn/docs/cn/api-reference/account/token-usage
 */
@Component
@ConfigurationProperties(prefix = "xq.toapis.usage")
@Data
public class XqToapisUsageProperties {

    private boolean enabled = true;

    /** 国内默认 toapis.cn */
    private String baseUrl = "https://toapis.cn";

    /**
     * 账户设置页「系统访问令牌」（不是 sk- API Key）。
     * 环境变量：XQ_TOAPIS_ACCESS_TOKEN
     */
    private String accessToken = "";

    /**
     * 账户设置页数字 User ID。
     * 环境变量：XQ_TOAPIS_USER_ID
     */
    private String userId = "";

    private int connectTimeoutMs = 8000;

    private int readTimeoutMs = 30000;

    /** 模型名包含以下关键词 → 文案生成计费 */
    private List<String> copyModelKeywords = new ArrayList<>(List.of(
            "terra", "gpt-5", "gpt-4", "claude", "chat"));

    /** 模型名包含以下关键词 → 生图计费（优先于改图关键词之外的默认图片） */
    private List<String> imageGenModelKeywords = new ArrayList<>(List.of(
            "gpt-image", "dall-e", "flux", "image"));

    /**
     * 模型名包含以下关键词 → 改图计费。
     * 若与生图共用同一模型，请单独建改图 Key/模型名，或后续接本地调用日志细分。
     */
    private List<String> imageReviseModelKeywords = new ArrayList<>(List.of(
            "image-edit", "image_edit", "edit", "revise"));
}
