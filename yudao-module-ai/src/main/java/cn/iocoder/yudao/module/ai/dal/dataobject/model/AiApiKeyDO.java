package cn.iocoder.yudao.module.ai.dal.dataobject.model;

import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * AI API 秘钥 DO。
 * <p>
 * 一条记录 = 一个接入源（OpenAI 官方，或某一个中转）。
 * 不同 {@link #gatewayType} 字段用法不同：官方可不填中转专有项；中转按站填写。
 */
@TableName("ai_api_key")
@KeySequence("ai_api_key_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiApiKeyDO extends BaseDO {

    @TableId
    private Long id;
    /** 显示名，如「OpenAI 官方」「Aixoras-主线」 */
    private String name;
    private String apiKey;
    /** 平台枚举 {@link AiPlatformEnum} */
    private String platform;
    /** Base URL；官方可填 https://api.openai.com/v1 */
    private String url;
    /**
     * 接入类型：openai_official / aixoras / hao / toapis / cun / openai_compatible / custom
     */
    private String gatewayType;
    /**
     * 能力列表，逗号分隔：chat,vision,image_gen,image_edit
     */
    private String capabilities;
    private String chatModel;
    private String visionModel;
    private String imageModel;
    private String imageEditModel;
    /** 生图请求风格（中转用）；官方可空 */
    private String imageBodyStyle;
    /** 异步生图；官方一般空/0 */
    private Integer supportsAsync;
    /** 优先 /v1/responses */
    private Integer preferResponsesApi;
    /** 识图 detail；空则调用方默认 */
    private String visionImageDetail;
    /** 扩展 JSON：各站私有参数 */
    private String extraConfig;
    private String remark;
    /** {@link CommonStatusEnum} */
    private Integer status;

}
