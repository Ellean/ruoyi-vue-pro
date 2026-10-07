package cn.iocoder.yudao.module.ai.service.chat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessagePageReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageRespVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendReqVO;
import cn.iocoder.yudao.module.ai.controller.admin.chat.vo.message.AiChatMessageSendRespVO;
import cn.iocoder.yudao.module.ai.dal.dataobject.chat.AiChatConversationDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.chat.AiChatMessageDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.knowledge.AiKnowledgeDocumentDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiChatRoleDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiToolDO;
import cn.iocoder.yudao.module.ai.dal.mysql.chat.AiChatMessageMapper;
import cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.ai.enums.model.AiPlatformEnum;
import cn.iocoder.yudao.module.ai.framework.ai.core.webserch.AiWebSearchClient;
import cn.iocoder.yudao.module.ai.framework.ai.core.webserch.AiWebSearchRequest;
import cn.iocoder.yudao.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import cn.iocoder.yudao.module.ai.service.knowledge.AiKnowledgeDocumentService;
import cn.iocoder.yudao.module.ai.service.knowledge.AiKnowledgeSegmentService;
import cn.iocoder.yudao.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchReqBO;
import cn.iocoder.yudao.module.ai.service.knowledge.bo.AiKnowledgeSegmentSearchRespBO;
import cn.iocoder.yudao.module.ai.service.model.AiChatRoleService;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import cn.iocoder.yudao.module.ai.service.model.AiToolService;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import cn.iocoder.yudao.module.ai.util.FileTypeUtils;
import com.google.common.collect.Maps;
import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.error;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.CHAT_CONVERSATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.CHAT_MESSAGE_NOT_EXIST;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.CHAT_MODEL_NO_VISION;

/**
 * AI 聊天消息 Service 实现类
 *
 * @author fansili
 */
@Service
@Slf4j
public class AiChatMessageServiceImpl implements AiChatMessageService {

    /**
     * 联网搜索的结束数
     */
    private static final Integer WEB_SEARCH_COUNT = 10;

    // TODO @芋艿：后续优化下对话的 Prompt 整体结构

    /**
     * 知识库转 {@link UserMessage} 的内容模版
     */
    private static final String KNOWLEDGE_USER_MESSAGE_TEMPLATE = "使用 <Reference></Reference> 标记中的内容作为本次对话的参考:\n\n" +
            "%s\n\n" + // 多个 <Reference></Reference> 的拼接
            "回答要求：\n- 避免提及你是从 <Reference></Reference> 获取的知识。";

    private static final String WEB_SEARCH_USER_MESSAGE_TEMPLATE = "使用 <WebSearch></WebSearch> 标记中的内容作为本次对话的参考:\n\n" +
            "%s\n\n" + // 多个 <WebSearch></WebSearch> 的拼接
            "回答要求：\n- 避免提及你是从 <WebSearch></WebSearch> 获取的知识。";

    /**
     * 附件转 ${@link UserMessage} 的内容模版
     */
    @SuppressWarnings("TextBlockMigration")
    private static final String Attachment_USER_MESSAGE_TEMPLATE = "使用 <Attachment></Attachment> 标记用户对话上传的附件内容:\n\n" +
            "%s\n\n" + // 多个 <Attachment></Attachment> 的拼接
            "回答要求：\n- 避免提及 <Attachment></Attachment> 附件的编码格式。";

    @Resource
    private AiChatMessageMapper chatMessageMapper;

    @Resource
    private AiChatConversationService chatConversationService;
    @Resource
    private AiChatRoleService chatRoleService;
    @Resource
    private AiModelService modalService;
    @Resource
    private AiKnowledgeSegmentService knowledgeSegmentService;
    @Resource
    private AiKnowledgeDocumentService knowledgeDocumentService;
    @Resource
    private AiToolService toolService;

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false) // 由于 yudao.ai.web-search.enable 配置项，可以关闭 AiWebSearchClient 的功能，所以这里只能不强制注入
    private AiWebSearchClient webSearchClient;

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false) // 由于 yudao.ai.mcp.client.enable 配置项，可以关闭 McpSyncClient 的功能，所以这里只能不强制注入
    private List<McpSyncClient> mcpClients;

    @Value("${spring.ai.mcp.client.name:mcp}")
    private String mcpClientName;

    @Resource
    private ToolCallbackResolver toolCallbackResolver;

    @Transactional(rollbackFor = Exception.class)
    public AiChatMessageSendRespVO sendMessage(AiChatMessageSendReqVO sendReqVO, Long userId) {
        // 1.1 校验对话存在
        AiChatConversationDO conversation = chatConversationService
                .validateChatConversationExists(sendReqVO.getConversationId());
        if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        List<AiChatMessageDO> historyMessages = chatMessageMapper.selectListByConversationId(conversation.getId());
        // 1.2 校验模型
        AiModelDO model = modalService.validateModel(conversation.getModelId());
        assertVisionModelIfHasImages(model, sendReqVO.getAttachmentUrls());
        ChatModel chatModel = modalService.getChatModel(model.getId());

        // 2.1 知识库召回
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = recallKnowledgeSegment(
                sendReqVO.getContent(), conversation);

        // 2.2 联网搜索
        AiWebSearchResponse webSearchResponse = Boolean.TRUE.equals(sendReqVO.getUseSearch()) && webSearchClient != null ?
                webSearchClient.search(new AiWebSearchRequest().setQuery(sendReqVO.getContent())
                        .setSummary(true).setCount(WEB_SEARCH_COUNT)) : null;

        // 3. 插入 user 发送消息
        AiChatMessageDO userMessage = createChatMessage(conversation.getId(), null, model,
                userId, conversation.getRoleId(), MessageType.USER, sendReqVO.getContent(), sendReqVO.getUseContext(),
                null, sendReqVO.getAttachmentUrls(), null);

        // 4.1 插入 assistant 接收消息
        AiChatMessageDO assistantMessage = createChatMessage(conversation.getId(), userMessage.getId(), model,
                userId, conversation.getRoleId(), MessageType.ASSISTANT, "", sendReqVO.getUseContext(),
                knowledgeSegments, null, webSearchResponse);

        // 4.2 创建 chat 需要的 Prompt
        Prompt prompt = buildPrompt(conversation, historyMessages, knowledgeSegments, webSearchResponse, model, sendReqVO);
        ChatResponse chatResponse = chatModel.call(prompt);

        // 4.3 更新响应内容
        String newContent = AiUtils.getChatResponseContent(chatResponse);
        String newReasoningContent = AiUtils.getChatResponseReasoningContent(chatResponse);
        chatMessageMapper.updateById(new AiChatMessageDO().setId(assistantMessage.getId())
                .setContent(newContent).setReasoningContent(newReasoningContent));
        // 4.4 响应结果
        Map<Long, AiKnowledgeDocumentDO> documentMap = knowledgeDocumentService.getKnowledgeDocumentMap(
                convertSet(knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getDocumentId));
        List<AiChatMessageRespVO.KnowledgeSegment> segments = BeanUtils.toBean(knowledgeSegments,
                AiChatMessageRespVO.KnowledgeSegment.class, segment -> {
                    AiKnowledgeDocumentDO document = documentMap.get(segment.getDocumentId());
                    segment.setDocumentName(document != null ? document.getName() : null);
                });
        return new AiChatMessageSendRespVO()
                .setSend(BeanUtils.toBean(userMessage, AiChatMessageSendRespVO.Message.class))
                .setReceive(BeanUtils.toBean(assistantMessage, AiChatMessageSendRespVO.Message.class)
                        .setContent(newContent).setSegments(segments)
                        .setWebSearchPages(webSearchResponse != null ? webSearchResponse.getLists() : null));
    }

    @Override
    public Flux<CommonResult<AiChatMessageSendRespVO>> sendChatMessageStream(AiChatMessageSendReqVO sendReqVO,
            Long userId) {
        // 1.1 校验对话存在
        AiChatConversationDO conversation = chatConversationService
                .validateChatConversationExists(sendReqVO.getConversationId());
        if (ObjUtil.notEqual(conversation.getUserId(), userId)) {
            throw exception(CHAT_CONVERSATION_NOT_EXISTS);
        }
        List<AiChatMessageDO> historyMessages = chatMessageMapper.selectListByConversationId(conversation.getId());
        // 1.2 校验模型
        AiModelDO model = modalService.validateModel(conversation.getModelId());
        // 1.3 带图时禁止选无视觉能力的模型（如 gpt-3.5-turbo），否则上游只报 Request failed
        assertVisionModelIfHasImages(model, sendReqVO.getAttachmentUrls());
        StreamingChatModel chatModel = modalService.getChatModel(model.getId());

        // 2.1 知识库找回
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = recallKnowledgeSegment(
                sendReqVO.getContent(), conversation);

        // 2.2 联网搜索
        AiWebSearchResponse webSearchResponse = Boolean.TRUE.equals(sendReqVO.getUseSearch()) && webSearchClient != null ?
                webSearchClient.search(new AiWebSearchRequest().setQuery(sendReqVO.getContent())
                        .setSummary(true).setCount(WEB_SEARCH_COUNT)) : null;

        // 3. 插入 user 发送消息
        AiChatMessageDO userMessage = createChatMessage(conversation.getId(), null, model,
                userId, conversation.getRoleId(), MessageType.USER, sendReqVO.getContent(), sendReqVO.getUseContext(),
                null, sendReqVO.getAttachmentUrls(), null);

        // 4.1 插入 assistant 接收消息
        AiChatMessageDO assistantMessage = createChatMessage(conversation.getId(), userMessage.getId(), model,
                userId, conversation.getRoleId(), MessageType.ASSISTANT, "", sendReqVO.getUseContext(),
                knowledgeSegments, null, webSearchResponse);

        // 4.2 构建 Prompt，并进行调用
        Prompt prompt = buildPrompt(conversation, historyMessages, knowledgeSegments, webSearchResponse, model, sendReqVO);
        Flux<ChatResponse> streamResponse = chatModel.stream(prompt);

        // 4.3 流式返回
        StringBuffer contentBuffer = new StringBuffer();
        StringBuffer reasoningContentBuffer = new StringBuffer();

        // 防止执行多次知识库和联网搜索
        AtomicBoolean firstExecuteFlag = new AtomicBoolean(true);
        AtomicReference<List<AiChatMessageRespVO.KnowledgeSegment>> cacheSegments = new AtomicReference<>();
        AtomicReference<List<AiWebSearchResponse.WebPage>> cacheWebSearchPages = new AtomicReference<>();
        return streamResponse.map(chunk -> {
            // 仅首次：返回知识库、联网搜索
            if (StrUtil.isEmpty(contentBuffer)) {
                if (firstExecuteFlag.compareAndSet(true, false)) { // CAS 操作，确保仅执行一次
                    Map<Long, AiKnowledgeDocumentDO> documentMap = TenantUtils.executeIgnore(() -> knowledgeDocumentService.getKnowledgeDocumentMap(
                            convertSet(knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getDocumentId)));
                    cacheSegments.set(BeanUtils.toBean(knowledgeSegments, AiChatMessageRespVO.KnowledgeSegment.class, segment -> {
                        AiKnowledgeDocumentDO document = documentMap.get(segment.getDocumentId());
                        segment.setDocumentName(document != null ? document.getName() : null);
                    }));
                    if (webSearchResponse != null) {
                        cacheWebSearchPages.set(webSearchResponse.getLists());
                    }
                }
            }
            // 响应结果
            String newContent = AiUtils.getChatResponseContent(chunk);
            String newReasoningContent = AiUtils.getChatResponseReasoningContent(chunk);
            if (StrUtil.isNotEmpty(newContent)) {
                contentBuffer.append(newContent);
            }
            if (StrUtil.isNotEmpty(newReasoningContent)) {
                reasoningContentBuffer.append(newReasoningContent);
            }
            return success(new AiChatMessageSendRespVO()
                    .setSend(BeanUtils.toBean(userMessage, AiChatMessageSendRespVO.Message.class))
                    .setReceive(BeanUtils.toBean(assistantMessage, AiChatMessageSendRespVO.Message.class)
                            .setContent(StrUtil.nullToDefault(newContent, "")) // 避免 null 的 情况
                            .setReasoningContent(StrUtil.nullToDefault(newReasoningContent, "")) // 避免 null 的 情况
                            .setSegments(cacheSegments.get()).setWebSearchPages(cacheWebSearchPages.get()))); // 知识库 + 联网搜索
        }).doOnComplete(() -> {
            // 忽略租户，因为 Flux 异步无法透传租户
            TenantUtils.executeIgnore(() -> chatMessageMapper.updateById(
                    new AiChatMessageDO().setId(assistantMessage.getId()).setContent(contentBuffer.toString())
                            .setReasoningContent(reasoningContentBuffer.toString())));
        }).doOnError(throwable -> {
            log.error("[sendChatMessageStream][userId({}) sendReqVO({}) 发生异常]", userId, sendReqVO, throwable);
            // 忽略租户，因为 Flux 异步无法透传租户
            TenantUtils.executeIgnore(() -> {
                // 如果有内容，则更新内容
                if (StrUtil.isNotEmpty(contentBuffer)) {
                    chatMessageMapper.updateById(new AiChatMessageDO().setId(assistantMessage.getId())
                            .setContent(contentBuffer.toString()).setReasoningContent(reasoningContentBuffer.toString()));
                } else {
                    // 否则，则进行删除
                    chatMessageMapper.deleteById(assistantMessage.getId());
                }
            });
        }).doOnCancel(() -> {
            log.info("[sendChatMessageStream][userId({}) sendReqVO({}) 取消请求]", userId, sendReqVO);
            // 忽略租户，因为 Flux 异步无法透传租户
            TenantUtils.executeIgnore(() -> {
                // 如果有内容，则更新内容
                if (StrUtil.isNotEmpty(contentBuffer)) {
                    chatMessageMapper.updateById(new AiChatMessageDO().setId(assistantMessage.getId())
                            .setContent(contentBuffer.toString()).setReasoningContent(reasoningContentBuffer.toString()));
                } else {
                    // 否则，则进行删除
                    chatMessageMapper.deleteById(assistantMessage.getId());
                }
            });
        }).onErrorResume(error -> {
            log.error("[sendChatMessageStream][userId({}) sendReqVO({}) 发生异常]", userId, sendReqVO, error);
            String detail = StrUtil.maxLength(resolveThrowableDetail(error), 220);
            return Flux.just(error(new cn.iocoder.yudao.framework.common.exception.ErrorCode(
                    ErrorCodeConstants.CHAT_STREAM_ERROR.getCode(),
                    StrUtil.isBlank(detail) ? "对话生成异常!" : ("对话生成异常! " + detail))));
        });
    }

    /** 上传图片时校验当前会话模型是否具备视觉能力 */
    private void assertVisionModelIfHasImages(AiModelDO model, List<String> attachmentUrls) {
        if (CollUtil.isEmpty(attachmentUrls) || model == null) {
            return;
        }
        boolean hasImage = false;
        for (String url : attachmentUrls) {
            if (StrUtil.isBlank(url)) {
                continue;
            }
            if (FileTypeUtils.isImage(FileTypeUtils.getMineType(FileNameUtil.getName(url)))) {
                hasImage = true;
                break;
            }
        }
        if (!hasImage) {
            return;
        }
        String name = StrUtil.blankToDefault(model.getModel(), "").toLowerCase();
        // 明确无视觉：gpt-3.5 / 纯文本 instruct 等
        boolean noVision = name.contains("gpt-3.5")
                || name.contains("instruct")
                || name.contains("text-embedding")
                || name.equals("gpt-4")
                || name.startsWith("gpt-4-0314")
                || name.startsWith("gpt-4-0613");
        if (noVision) {
            throw exception(CHAT_MODEL_NO_VISION, model.getModel());
        }
    }

    private static String resolveThrowableDetail(Throwable error) {
        if (error == null) {
            return "";
        }
        String best = StrUtil.blankToDefault(error.getMessage(), "");
        Throwable cur = error;
        int depth = 0;
        while (cur != null && depth++ < 8) {
            String msg = StrUtil.blankToDefault(cur.getMessage(), "");
            if (StrUtil.isNotBlank(msg) && !StrUtil.equalsIgnoreCase(msg, "Request failed")) {
                best = msg;
            }
            // OpenAI Java SDK：尽量带上 HTTP 状态
            try {
                var method = cur.getClass().getMethod("statusCode");
                Object code = method.invoke(cur);
                if (code != null && StrUtil.isNotBlank(msg)) {
                    best = code + " " + msg;
                }
            } catch (ReflectiveOperationException ignored) {
                // ignore
            }
            cur = cur.getCause();
        }
        if (StrUtil.containsIgnoreCase(best, "timeout")
                || StrUtil.containsIgnoreCase(best, "InterruptedIOException")) {
            return "上游识图请求超时，请稍后重试；若持续出现请检查中转站连通性或换更快的视觉模型。原始错误：" + best;
        }
        if (StrUtil.containsIgnoreCase(best, "Request failed")
                || StrUtil.containsIgnoreCase(best, "OpenAIIOException")) {
            best = best + "（请确认：1) 右上角已选带视觉模型如 gpt-5.6-terra；2) API 密钥 URL/Key 可用；3) 服务端能访问图片地址）";
        }
        return best;
    }

    private List<AiKnowledgeSegmentSearchRespBO> recallKnowledgeSegment(String content,
            AiChatConversationDO conversation) {
        // 1. 查询聊天角色
        if (conversation == null || conversation.getRoleId() == null) {
            return Collections.emptyList();
        }
        AiChatRoleDO role = chatRoleService.getChatRole(conversation.getRoleId());
        if (role == null || CollUtil.isEmpty(role.getKnowledgeIds())) {
            return Collections.emptyList();
        }

        // 2. 遍历召回
        List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments = new ArrayList<>();
        for (Long knowledgeId : role.getKnowledgeIds()) {
            knowledgeSegments.addAll(knowledgeSegmentService.searchKnowledgeSegment(new AiKnowledgeSegmentSearchReqBO()
                    .setKnowledgeId(knowledgeId).setContent(content)));
        }
        return knowledgeSegments;
    }

    private Prompt buildPrompt(AiChatConversationDO conversation, List<AiChatMessageDO> messages,
                               List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
                               AiWebSearchResponse webSearchResponse,
                               AiModelDO model, AiChatMessageSendReqVO sendReqVO) {
        List<Message> chatMessages = new ArrayList<>();
        // 1.1 System Context 角色设定
        if (StrUtil.isNotBlank(conversation.getSystemMessage())) {
            chatMessages.add(new SystemMessage(conversation.getSystemMessage()));
        }

        // 1.2 历史 history message 历史消息（用户消息含图时走多模态 Media，不再把 Base64 当纯文本）
        List<AiChatMessageDO> contextMessages = filterContextMessages(messages, conversation, sendReqVO);
        contextMessages.forEach(message -> {
            if (MessageType.USER.getValue().equals(message.getType())) {
                chatMessages.add(buildUserMessageWithAttachments(message.getContent(), message.getAttachmentUrls()));
            } else {
                chatMessages.add(AiUtils.buildMessage(message.getType(), message.getContent()));
            }
            // TODO @芋艿：历史的知识库；历史的搜索，要不要拼接？
        });

        // 1.3 当前 user message：文字 + 图片同一条多模态消息（识图必需）
        chatMessages.add(buildUserMessageWithAttachments(sendReqVO.getContent(), sendReqVO.getAttachmentUrls()));

        // 1.4 知识库，通过 UserMessage 实现
        if (CollUtil.isNotEmpty(knowledgeSegments)) {
            String reference = knowledgeSegments.stream()
                    .map(segment -> "<Reference>" + segment.getContent() + "</Reference>")
                    .collect(Collectors.joining("\n\n"));
            chatMessages.add(new UserMessage(String.format(KNOWLEDGE_USER_MESSAGE_TEMPLATE, reference)));
        }

        // 1.5 联网搜索，通过 UserMessage 实现
        if (webSearchResponse != null && CollUtil.isNotEmpty(webSearchResponse.getLists())) {
            String webSearch = webSearchResponse.getLists().stream()
                    .map(page -> {
                        String summary = StrUtil.isNotEmpty(page.getSummary()) ?
                                "\nSummary: " + page.getSummary() : "";
                        return "<WebSearch title=\"" + page.getTitle() + "\" url=\"" + page.getUrl() + "\">"
                                + StrUtil.blankToDefault(page.getSummary(), page.getSnippet()) + "</WebSearch>";
                    })
                    .collect(Collectors.joining("\n\n"));
            chatMessages.add(new UserMessage(String.format(WEB_SEARCH_USER_MESSAGE_TEMPLATE, webSearch)));
        }

        // 2.1 查询 tool 工具
        List<ToolCallback> toolCallbacks = getToolCallbackListByRoleId(conversation.getRoleId());
        Map<String,Object> toolContext = CollUtil.isNotEmpty(toolCallbacks) ? AiUtils.buildCommonToolContext()
                : Map.of();
        // 2.2 构建 ChatOptions 对象
        AiPlatformEnum platform = AiPlatformEnum.validatePlatform(model.getPlatform());
        ChatOptions chatOptions = AiUtils.buildChatOptions(platform, model.getModel(),
                conversation.getTemperature(), conversation.getMaxTokens(),
                toolCallbacks, toolContext);
        return new Prompt(chatMessages, chatOptions);
    }

    private List<ToolCallback> getToolCallbackListByRoleId(Long roleId) {
        if (roleId == null) {
            return null;
        }
        AiChatRoleDO chatRole = chatRoleService.getChatRole(roleId);
        if (chatRole == null) {
            return null;
        }
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        // 1. 通过 toolIds
        if (CollUtil.isNotEmpty(chatRole.getToolIds())) {
            Set<String> toolNames = convertSet(toolService.getToolList(chatRole.getToolIds()), AiToolDO::getName);
            toolNames.forEach(toolName -> {
                ToolCallback toolCallback = toolCallbackResolver.resolve(toolName);
                if (toolCallback != null) {
                    toolCallbacks.add(toolCallback);
                }
            });
        }
        // 2. 通过 mcpClients
        if (CollUtil.isNotEmpty(mcpClients) && CollUtil.isNotEmpty(chatRole.getMcpClientNames())) {
            chatRole.getMcpClientNames().forEach(mcpClientName -> {
                // 2.1 标准化名字，参考 McpClientAutoConfiguration 的 connectedClientName 方法
                String finalMcpClientName = this.mcpClientName + " - " + mcpClientName;
                // 2.2 匹配对应的 McpSyncClient
                mcpClients.forEach(mcpClient -> {
                    if (ObjUtil.notEqual(mcpClient.getClientInfo().name(), finalMcpClientName)) {
                        return;
                    }
                    ToolCallback[] mcpToolCallBacks = SyncMcpToolCallbackProvider.builder()
                            .mcpClients(mcpClient)
                            .build()
                            .getToolCallbacks();
                    CollUtil.addAll(toolCallbacks, mcpToolCallBacks);
                });
            });
        }
        return toolCallbacks;
    }

    /**
     * 从历史消息中，获得倒序的 n 组消息作为消息上下文
     * <p>
     * n 组：指的是 user + assistant 形成一组
     *
     * @param messages     消息列表
     * @param conversation 对话
     * @param sendReqVO    发送请求
     * @return 消息上下文
     */
    private List<AiChatMessageDO> filterContextMessages(List<AiChatMessageDO> messages,
            AiChatConversationDO conversation,
            AiChatMessageSendReqVO sendReqVO) {
        if (conversation.getMaxContexts() == null || ObjUtil.notEqual(sendReqVO.getUseContext(), Boolean.TRUE)) {
            return Collections.emptyList();
        }
        List<AiChatMessageDO> contextMessages = new ArrayList<>(conversation.getMaxContexts() * 2);
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatMessageDO assistantMessage = CollUtil.get(messages, i);
            if (assistantMessage == null || assistantMessage.getReplyId() == null) {
                continue;
            }
            AiChatMessageDO userMessage = CollUtil.get(messages, i - 1);
            if (userMessage == null
                    || ObjUtil.notEqual(assistantMessage.getReplyId(), userMessage.getId())
                    || StrUtil.isEmpty(assistantMessage.getContent())) {
                continue;
            }
            // 由于后续要 reverse 反转，所以先添加 assistantMessage
            contextMessages.add(assistantMessage);
            contextMessages.add(userMessage);
            // 超过最大上下文，结束
            if (contextMessages.size() >= conversation.getMaxContexts() * 2) {
                break;
            }
        }
        Collections.reverse(contextMessages);
        return contextMessages;
    }

    /**
     * 构建用户消息：图片走 Spring AI {@link Media} 多模态；文档仍拼文本 Attachment。
     * 旧实现把图片 Base64 塞进纯文本，gpt-5.6-terra 等识图模型无法真正看图，易直接对话异常。
     */
    private UserMessage buildUserMessageWithAttachments(String text, List<String> attachmentUrls) {
        String content = StrUtil.blankToDefault(text, "");
        if (CollUtil.isEmpty(attachmentUrls)) {
            return new UserMessage(content);
        }
        List<Media> imageMedias = new ArrayList<>();
        Map<String, String> docContents = Maps.newLinkedHashMapWithExpectedSize(attachmentUrls.size());
        for (String attachmentUrl : attachmentUrls) {
            if (StrUtil.isBlank(attachmentUrl)) {
                continue;
            }
            try {
                String name = FileNameUtil.getName(attachmentUrl);
                String mimeType = FileTypeUtils.getMineType(name);
                if (FileTypeUtils.isImage(mimeType)) {
                    byte[] bytes = HttpUtil.downloadBytes(attachmentUrl);
                    if (bytes == null || bytes.length == 0) {
                        log.warn("[buildUserMessageWithAttachments][图片为空 url={}]", attachmentUrl);
                        continue;
                    }
                    if (bytes.length > 8 * 1024 * 1024) {
                        log.warn("[buildUserMessageWithAttachments][图片过大 {}KB url={}，仍尝试发送]",
                                bytes.length / 1024, attachmentUrl);
                    }
                    String safeMime = StrUtil.blankToDefault(mimeType, "image/jpeg");
                    imageMedias.add(Media.builder()
                            .mimeType(MimeTypeUtils.parseMimeType(safeMime))
                            .data(new ByteArrayResource(bytes))
                            .build());
                } else {
                    String doc = knowledgeDocumentService.readUrl(attachmentUrl);
                    if (StrUtil.isNotEmpty(doc)) {
                        docContents.put(name, doc);
                    }
                }
            } catch (Exception e) {
                log.error("[buildUserMessageWithAttachments][读取附件({}) 发生异常]", attachmentUrl, e);
            }
        }
        if (CollUtil.isNotEmpty(docContents)) {
            String attachment = docContents.entrySet().stream()
                    .map(entry -> "<Attachment name=\"" + entry.getKey() + "\">" + entry.getValue() + "</Attachment>")
                    .collect(Collectors.joining("\n\n"));
            content = content + "\n\n" + String.format(Attachment_USER_MESSAGE_TEMPLATE, attachment);
        }
        if (CollUtil.isEmpty(imageMedias)) {
            if (CollUtil.isNotEmpty(attachmentUrls) && CollUtil.isEmpty(docContents)) {
                // 有附件但全部失败：明确提示，避免模型空答
                content = content + "\n\n(系统提示：附件图片未能加载，请检查文件地址是否可被服务端访问)";
            }
            return new UserMessage(content);
        }
        return UserMessage.builder()
                .text(StrUtil.blankToDefault(content, "请根据图片回答用户问题。"))
                .media(imageMedias)
                .build();
    }

    private AiChatMessageDO createChatMessage(Long conversationId, Long replyId,
                                              AiModelDO model, Long userId, Long roleId,
                                              MessageType messageType, String content, Boolean useContext,
                                              List<AiKnowledgeSegmentSearchRespBO> knowledgeSegments,
                                              List<String> attachmentUrls,
                                              AiWebSearchResponse webSearchResponse) {
        AiChatMessageDO message = new AiChatMessageDO().setConversationId(conversationId).setReplyId(replyId)
                .setModel(model.getModel()).setModelId(model.getId()).setUserId(userId).setRoleId(roleId)
                .setType(messageType.getValue()).setContent(content).setUseContext(useContext)
                .setSegmentIds(convertList(knowledgeSegments, AiKnowledgeSegmentSearchRespBO::getId))
                .setAttachmentUrls(attachmentUrls);
        if (webSearchResponse != null) {
            message.setWebSearchPages(webSearchResponse.getLists());
        }
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public List<AiChatMessageDO> getChatMessageListByConversationId(Long conversationId) {
        return chatMessageMapper.selectListByConversationId(conversationId);
    }

    @Override
    public AiChatMessageDO getChatMessage(Long id) {
        return chatMessageMapper.selectById(id);
    }

    @Override
    public void deleteChatMessage(Long id, Long userId) {
        // 1. 校验消息存在
        AiChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null || ObjUtil.notEqual(message.getUserId(), userId)) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        // 2. 执行删除
        chatMessageMapper.deleteById(id);
    }

    @Override
    public void deleteChatMessageByConversationId(Long conversationId, Long userId) {
        // 1. 校验消息存在
        List<AiChatMessageDO> messages = chatMessageMapper.selectListByConversationId(conversationId);
        if (CollUtil.isEmpty(messages) || ObjUtil.notEqual(messages.getFirst().getUserId(), userId)) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        // 2. 执行删除
        chatMessageMapper.deleteByIds(convertList(messages, AiChatMessageDO::getId));
    }

    @Override
    public void deleteChatMessageByAdmin(Long id) {
        // 1. 校验消息存在
        AiChatMessageDO message = chatMessageMapper.selectById(id);
        if (message == null) {
            throw exception(CHAT_MESSAGE_NOT_EXIST);
        }
        // 2. 执行删除
        chatMessageMapper.deleteById(id);
    }

    @Override
    public Integer getChatMessageCount(Long conversationId) {
        return chatMessageMapper.selectCount(AiChatMessageDO::getConversationId, conversationId).intValue();
    }

    @Override
    public Map<Long, Integer> getChatMessageCountMap(Collection<Long> conversationIds) {
        return chatMessageMapper.selectCountMapByConversationId(conversationIds);
    }

    @Override
    public PageResult<AiChatMessageDO> getChatMessagePage(AiChatMessagePageReqVO pageReqVO) {
        return chatMessageMapper.selectPage(pageReqVO);
    }

}
