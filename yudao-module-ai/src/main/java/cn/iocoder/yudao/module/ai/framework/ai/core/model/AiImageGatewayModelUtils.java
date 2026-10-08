package cn.iocoder.yudao.module.ai.framework.ai.core.model;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiApiKeyDO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 中转站图片模型名规范化与 503 无通道时的候选列表。
 * <p>
 * 对齐 xq-erp：Hao 需 {@code openai/} 前缀；裸 {@code gpt-image-2.5} 无通道。
 */
public final class AiImageGatewayModelUtils {

    private static final Pattern CONCRETE_25 = Pattern.compile(
            "^gpt-image-2\\.5-(flare|sunburst)(?:-\\d{4}-\\d{2}-\\d{2})?$",
            Pattern.CASE_INSENSITIVE);

    private AiImageGatewayModelUtils() {
    }

    public static String bareModel(String model) {
        String name = StrUtil.trim(model);
        if (StrUtil.isBlank(name)) {
            return "";
        }
        if (StrUtil.startWithIgnoreCase(name, "openai/")) {
            return name.substring("openai/".length());
        }
        return name;
    }

    public static boolean isGptImageModel(String model) {
        return StrUtil.startWithIgnoreCase(bareModel(model), "gpt-image");
    }

    public static boolean isHaoGateway(AiApiKeyDO apiKey) {
        if (apiKey == null) {
            return false;
        }
        if (StrUtil.equalsIgnoreCase(apiKey.getGatewayType(), "hao")) {
            return true;
        }
        String url = StrUtil.blankToDefault(apiKey.getUrl(), "").toLowerCase(Locale.ROOT);
        return url.contains("hao.ai") || url.contains("api.hao.");
    }

    /**
     * 按密钥所属中转纠正模型名（调用前规范化）。
     */
    public static String normalizeImageModel(AiApiKeyDO apiKey, String model) {
        String name = StrUtil.trim(model);
        String bare = bareModel(name);
        boolean hao = isHaoGateway(apiKey);

        // 密钥上配置的默认生图模型优先补洞
        String keyDefault = StrUtil.trim(apiKey != null ? apiKey.getImageModel() : null);
        if (StrUtil.isBlank(bare)
                || bare.matches("(?i)gpt-image-2\\.5")
                || bare.matches("(?i)gptimage2(\\.5)?")
                || bare.matches("(?i)gpt-image2(\\.5)?")) {
            name = StrUtil.blankToDefault(keyDefault, "gpt-image-2.5-flare");
            bare = bareModel(name);
        }

        if (hao && isGptImageModel(name) && !name.contains("/")) {
            return "openai/" + bareModel(name);
        }
        // 非 Hao：去掉错误的 openai/ 前缀（部分站不认）
        if (!hao && StrUtil.startWithIgnoreCase(name, "openai/") && isGptImageModel(name)) {
            return bareModel(name);
        }
        return name;
    }

    /**
     * 503 No available channel 时依次尝试的候选模型（已规范化、去重）。
     */
    public static List<String> channelFailoverCandidates(AiApiKeyDO apiKey, String requestedModel) {
        Set<String> out = new LinkedHashSet<>();
        String primary = normalizeImageModel(apiKey, requestedModel);
        if (StrUtil.isNotBlank(primary)) {
            out.add(primary);
        }
        if (apiKey != null && StrUtil.isNotBlank(apiKey.getImageModel())) {
            out.add(normalizeImageModel(apiKey, apiKey.getImageModel()));
        }
        boolean hao = isHaoGateway(apiKey);
        String[] fallbacks = {
                "gpt-image-2.5-flare",
                "gpt-image-2.5-sunburst",
                "gpt-image-2",
                "gpt-image-1.5",
                "gpt-image-1",
        };
        for (String fb : fallbacks) {
            out.add(normalizeImageModel(apiKey, fb));
            if (hao) {
                out.add("openai/" + fb);
            }
        }
        // 去掉空
        out.removeIf(StrUtil::isBlank);
        return new ArrayList<>(out);
    }

    public static boolean isNoAvailableChannel(Throwable ex) {
        String msg = ex == null ? "" : StrUtil.blankToDefault(ex.getMessage(), "");
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("no available channel")
                || (lower.contains("503") && lower.contains("channel"));
    }

    public static boolean looksLikeConcrete25(String model) {
        return CONCRETE_25.matcher(bareModel(model)).matches();
    }
}
