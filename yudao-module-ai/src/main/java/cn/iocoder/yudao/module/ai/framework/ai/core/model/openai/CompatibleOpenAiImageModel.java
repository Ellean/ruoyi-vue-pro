package cn.iocoder.yudao.module.ai.framework.ai.core.model.openai;

import cn.hutool.core.util.StrUtil;
import com.openai.client.OpenAIClient;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.metadata.OpenAiImageGenerationMetadata;
import org.springframework.ai.openai.metadata.OpenAiImageResponseMetadata;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * 兼容中转站返回 {@code "url":""} + {@code b64_json} 的 OpenAI 生图模型。
 * <p>
 * Spring AI {@link OpenAiImageModel} 用 {@code url().isPresent()} 判断，空字符串也会走 url 分支并把 b64 丢掉，
 * 随后业务侧 {@code HttpUtil.downloadBytes("")} 报 {@code [url] is blank !}。
 */
public class CompatibleOpenAiImageModel implements ImageModel {

    private final OpenAiImageOptions defaultOptions;
    private final OpenAIClient openAiClient;

    public CompatibleOpenAiImageModel(OpenAiImageOptions options) {
        Assert.notNull(options, "options cannot be null");
        this.defaultOptions = options;
        Integer maxRetries = options.getMaxRetries();
        this.openAiClient = OpenAiSetup.setupSyncClient(
                options.getBaseUrl(),
                options.getApiKey(),
                options.getCredential(),
                options.getMicrosoftDeploymentName(),
                options.getMicrosoftFoundryServiceVersion(),
                options.getOrganizationId(),
                options.isMicrosoftFoundry(),
                options.isGitHubModels(),
                options.getModel(),
                options.getTimeout(),
                maxRetries != null ? maxRetries : 0,
                options.getProxy(),
                options.getCustomHeaders(),
                ObservationRegistry.NOOP,
                null,
                Collections.emptyList());
    }

    @Override
    public ImageResponse call(ImagePrompt imagePrompt) {
        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .from(this.defaultOptions)
                .merge(imagePrompt.getOptions())
                .build();
        ImageGenerateParams params = options.toOpenAiImageGenerateParams(imagePrompt);
        ImagesResponse images = this.openAiClient.images().generate(params);
        if (images.data().isEmpty() || images.data().get().isEmpty()) {
            throw new IllegalArgumentException("Image generation failed: no image returned");
        }
        List<ImageGeneration> generations = images.data().get().stream().map(nativeImage -> {
            String url = nativeImage.url().orElse(null);
            String b64 = nativeImage.b64Json().orElse(null);
            Image image;
            // 优先非空 url；空 url 时改用 b64（Aixoras gpt-image 常见）
            if (StrUtil.isNotBlank(url)) {
                image = new Image(url, null);
            } else if (StrUtil.isNotEmpty(b64)) {
                image = new Image(null, b64);
            } else {
                throw new IllegalArgumentException(
                        "Image generation failed: image entry missing url and b64_json");
            }
            return new ImageGeneration(image,
                    new OpenAiImageGenerationMetadata(nativeImage.revisedPrompt().orElse(null)));
        }).toList();
        return new ImageResponse(generations, OpenAiImageResponseMetadata.from(images));
    }

}
