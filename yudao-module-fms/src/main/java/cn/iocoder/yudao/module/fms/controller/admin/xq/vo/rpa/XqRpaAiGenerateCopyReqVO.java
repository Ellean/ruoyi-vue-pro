package cn.iocoder.yudao.module.fms.controller.admin.xq.vo.rpa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "RPA AI - 生成文案请求")
@Data
public class XqRpaAiGenerateCopyReqVO {

    @Schema(description = "与 xq.rpa.callback-token 一致")
    private String callbackToken;

    private Long workOrderId;
    private String sku;
    private String title;
    private String originalCopy;
    /** RPA 本机首图识图结果（文案阶段只用首图，不是全套图） */
    private Map<String, Object> imageObservations;
    /** 文案阶段建议只传首图 URL；全套图留给后续提示词二创 */
    private List<String> sourceImages;
    /** { platformId, name, configJson } */
    private Map<String, Object> copyRule;
    /** 强制 mock（无密钥时也会自动 mock） */
    private Boolean mock;

    /** 是否二次改写：参考现有文案 + 用户提示词 */
    private Boolean regenerate;
    /** 用户二次修改提示词 */
    private String revisionPrompt;
    /** 当前已有文案 { title, sellingPoints, description } */
    private Map<String, Object> existingCopy;
    /** 修改范围：all / title / description / feature */
    private String target;
    /** 卖点下标（从 0 起，target=feature 时有效） */
    private Integer featureIndex;

}
