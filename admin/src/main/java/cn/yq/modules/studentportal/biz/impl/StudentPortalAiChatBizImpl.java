package cn.yq.modules.studentportal.biz.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.yq.common.dataConfig.service.SystemConfig;
import cn.yq.common.dataConfig.service.SystemConfigService;
import cn.yq.common.redis.RedisKeyConstants;
import cn.yq.config.PythonAgentProperties;
import cn.yq.common.exception.BusinessException;
import com.alibaba.fastjson2.JSON;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalAiChatBiz;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.entity.aichat.ChatMessageEntity;
import cn.yq.modules.studentportal.domain.entity.aichat.ChatSessionEntity;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentAiChatCreateSessionReq;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentAiChatSendMessageReq;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentChatPythonSummarizeReq;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentChatPythonStreamReq;
import cn.yq.modules.studentportal.domain.vo.res.aichat.PythonStudentChatStreamEvent;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatMessageRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatSendMessageRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatSessionRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatStreamEvent;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentChatPythonSummarizeRes;
import cn.yq.modules.studentportal.mapper.ChatMessageMapper;
import cn.yq.modules.studentportal.mapper.ChatSessionMapper;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.domain.entity.CourseEntity;
import cn.yq.modules.teaching.mapper.ClassMapper;
import cn.yq.modules.teaching.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentPortalAiChatBizImpl implements StudentPortalAiChatBiz {

    private static final String SESSION_STATUS_ACTIVE = "ACTIVE";
    private static final String ARCHIVE_STATUS_NONE = "NONE";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String CONTENT_TYPE_MARKDOWN = "markdown";
    private static final String MOCK_MODEL_NAME = "mock-student-assistant";
    private static final String PYTHON_MODEL_NAME = "python-student-agent";
    private static final long SUMMARY_COMPRESS_LOCK_LEASE_MINUTES = 5L;

    private final StudentService studentService;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final PythonAgentProperties pythonAgentProperties;
    private final SystemConfigService systemConfigService;
    private final RedissonClient redissonClient;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentAiChatSessionRes createSession(StudentAiChatCreateSessionReq request) {
        StudentEntity student = requireCurrentStudent();
        Date now = new Date();

        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setStudentId(student.getId());
        entity.setStudentCode(student.getStudentCode());
        entity.setTitle(normalizeTitle(request == null ? null : request.getTitle()));
        entity.setStatus(SESSION_STATUS_ACTIVE);
        entity.setMessageCount(0);
        entity.setLastMessageAt(now);
        entity.setSummary("");
        entity.setSummaryTokenCount(0);
        entity.setCompressedUntilMessageId(null);
        entity.setArchiveStatus(ARCHIVE_STATUS_NONE);
        entity.setArchiveId(null);
        entity.setExpireAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        chatSessionMapper.insert(entity);
        return toSessionRes(entity);
    }

    @Override
    public List<StudentAiChatSessionRes> listSessions() {
        StudentEntity student = requireCurrentStudent();
        return chatSessionMapper.selectByStudentId(student.getId()).stream()
                .map(this::toSessionRes)
                .toList();
    }

    @Override
    public List<StudentAiChatMessageRes> listMessages(Long sessionId) {
        StudentEntity student = requireCurrentStudent();
        requireSession(sessionId, student.getId());
        return chatMessageMapper.selectBySessionIdAndStudentId(sessionId, student.getId()).stream()
                .map(this::toMessageRes)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentAiChatSendMessageRes sendMessage(Long sessionId, StudentAiChatSendMessageReq request) {
        StudentEntity student = requireCurrentStudent();
        ChatSessionEntity session = requireSession(sessionId, student.getId());
        String content = StrUtil.trim(request.getContent());
        if (StrUtil.isBlank(content)) {
            throw new BusinessException("消息内容不能为空");
        }

        Date now = new Date();
        ChatMessageEntity userMessage = buildMessage(session.getId(), student.getId(), ROLE_USER, content, null, "stop", now);
        chatMessageMapper.insert(userMessage);

        String answer = buildMockAnswer(content);
        ChatMessageEntity assistantMessage = buildMessage(session.getId(), student.getId(), ROLE_ASSISTANT, answer, MOCK_MODEL_NAME, "stop", new Date());
        chatMessageMapper.insert(assistantMessage);

        chatSessionMapper.updateTitleIfBlank(session.getId(), student.getId(), buildTitle(content), now);
        chatSessionMapper.updateStats(session.getId(), student.getId(), assistantMessage.getCreatedAt(), assistantMessage.getCreatedAt());
        triggerSummaryCompressionAsync(session.getId(), student.getId());

        StudentAiChatSendMessageRes response = new StudentAiChatSendMessageRes();
        response.setUserMessage(toMessageRes(userMessage));
        response.setAssistantMessage(toMessageRes(assistantMessage));
        return response;
    }

    @Override
    public void streamMessage(Long sessionId, StudentAiChatSendMessageReq request, Consumer<StudentAiChatStreamEvent> eventConsumer) {
        // 流式接口仍然由 Java 做业务落库：先确认当前学生和会话归属，
        // 避免 Python Agent 以后接入后直接碰业务库或绕过学生权限。
        StudentEntity student = requireCurrentStudent();
        ChatSessionEntity session = requireSession(sessionId, student.getId());
        String content = StrUtil.trim(request.getContent());
        if (StrUtil.isBlank(content)) {
            throw new BusinessException("消息内容不能为空");
        }

        // 用户消息先落库，再通知前端展示正式的 user message。
        // 这样即使后续模型生成失败，学生的问题也不会丢失。
        Date now = new Date();
        ChatMessageEntity userMessage = buildMessage(session.getId(), student.getId(), ROLE_USER, content, null, "stop", now);
        chatMessageMapper.insert(userMessage);
        eventConsumer.accept(StudentAiChatStreamEvent.of("USER_MESSAGE_SAVED", "用户消息已保存", toMessageRes(userMessage)));

        eventConsumer.accept(StudentAiChatStreamEvent.of("THINKING", "正在整理回答", null));

        // 第三阶段开始，Java 不再自己生成 mock 回答，而是把结构化上下文发给 Python。
        // Python 返回一行一个 JSON 的 NDJSON 流。Java 读到 TOKEN 后立刻转发给前端。
        StringBuilder answerBuffer = new StringBuilder();
        String finishReason = callPythonStudentChatStream(session, userMessage, student, content, answerBuffer, eventConsumer);

        // 流结束后再保存助手完整回答。
        // 注意：前端已经实时看到了 token，但 MySQL 仍然需要保存完整消息，供历史记录和后续上下文使用。
        ChatMessageEntity assistantMessage = buildMessage(
                session.getId(),
                student.getId(),
                ROLE_ASSISTANT,
                answerBuffer.toString(),
                PYTHON_MODEL_NAME,
                finishReason,
                new Date()
        );
        chatMessageMapper.insert(assistantMessage);
        chatSessionMapper.updateTitleIfBlank(session.getId(), student.getId(), buildTitle(content), now);
        chatSessionMapper.updateStats(session.getId(), student.getId(), assistantMessage.getCreatedAt(), assistantMessage.getCreatedAt());

        eventConsumer.accept(StudentAiChatStreamEvent.of("MESSAGE_DONE", "回答已保存", toMessageRes(assistantMessage)));
        eventConsumer.accept(StudentAiChatStreamEvent.of("DONE", "完成", null));

        // 摘要压缩不阻塞学生端流式体验；失败只记录日志，不影响本轮回答。
        triggerSummaryCompressionAsync(session.getId(), student.getId());
    }

    /**
     * 调用 Python 学生问答流式接口，并把 Python token 转成前端 SSE 事件。
     *
     * <p>边界约定：
     * 1. Java 负责学生鉴权、会话归属、消息落库；
     * 2. Python 负责 AI 生成，当前先 mock，后续接 LLM/RAG；
     * 3. Python 返回 NDJSON，每一行是一个完整 JSON 事件；
     * 4. Java 收到 TOKEN 后立刻转发前端，同时追加到 answerBuffer，最后由 Java 保存完整 assistant 消息。</p>
     */
    private String callPythonStudentChatStream(ChatSessionEntity session,
                                               ChatMessageEntity userMessage,
                                               StudentEntity student,
                                               String content,
                                               StringBuilder answerBuffer,
                                               Consumer<StudentAiChatStreamEvent> eventConsumer) {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled()) || !StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            return fallbackWhenPythonUnavailable(answerBuffer, eventConsumer, "Python Agent 未启用或未配置");
        }

        StudentChatPythonStreamReq request = buildPythonStreamReq(session, userMessage, student, content);
        String url = buildPythonStudentChatStreamUrl();
        String requestJson = JSON.toJSONString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // Java HttpClient 默认可能尝试 HTTP/2 upgrade。
                // Uvicorn/FastAPI 当前接口按普通 HTTP/1.1 流式响应处理，强制 HTTP_1_1 可以避免
                // Python 服务端出现 "Unsupported upgrade request" 这类无关告警。
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofMillis(pythonAgentProperties.getTimeoutMs()))
                .header("Accept", "application/x-ndjson")
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<InputStream> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                return fallbackWhenPythonUnavailable(answerBuffer, eventConsumer, "Python Agent 返回异常状态：" + response.statusCode());
            }
            return forwardPythonStream(response.body(), answerBuffer, eventConsumer);
        } catch (Exception ex) {
            return fallbackWhenPythonUnavailable(answerBuffer, eventConsumer, "Python Agent 调用失败：" + ex.getMessage());
        }
    }

    /**
     * 逐行读取 Python NDJSON 流。
     *
     * <p>Python 每 yield 一行，Java 这里就能读到一行。
     * TOKEN 事件会被马上转成前端 SSE 的 TOKEN 事件，所以用户能看到实时输出。</p>
     */
    private String forwardPythonStream(InputStream inputStream,
                                       StringBuilder answerBuffer,
                                       Consumer<StudentAiChatStreamEvent> eventConsumer) throws IOException {
        String finishReason = "stop";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                PythonStudentChatStreamEvent pythonEvent = JSON.parseObject(line, PythonStudentChatStreamEvent.class);
                String eventType = pythonEvent.getType();
                if ("TOKEN".equals(eventType)) {
                    String delta = pythonEvent.getDelta();
                    if (StrUtil.isBlank(delta)) {
                        continue;
                    }
                    answerBuffer.append(delta);

                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("delta", delta);
                    eventConsumer.accept(StudentAiChatStreamEvent.of("TOKEN", null, data));
                    continue;
                }

                if ("DONE".equals(eventType)) {
                    finishReason = StrUtil.blankToDefault(pythonEvent.getFinishReason(), "stop");
                    continue;
                }

                if ("ERROR".equals(eventType)) {
                    throw new BusinessException(StrUtil.blankToDefault(pythonEvent.getMessage(), "Python Agent 生成失败"));
                }
            }
        }
        return finishReason;
    }

    /**
     * Python 不可用时的兜底。
     *
     * <p>这里仍然按 TOKEN 推给前端，并保存 assistant message，只是 finish_reason 记为 error。
     * 这样学生不会面对空白页面，刷新历史时也能看到这次失败提示。</p>
     */
    private String fallbackWhenPythonUnavailable(StringBuilder answerBuffer,
                                                 Consumer<StudentAiChatStreamEvent> eventConsumer,
                                                 String reason) {
        String fallback = "AI 服务暂时不可用，请稍后再试。\n\n" + reason;
        answerBuffer.append(fallback);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("delta", fallback);
        eventConsumer.accept(StudentAiChatStreamEvent.of("TOKEN", null, data));
        eventConsumer.accept(StudentAiChatStreamEvent.of("ERROR", reason, null));
        return "error";
    }

    /**
     * 构造 Java 发给 Python 的结构化请求。
     *
     * <p>当前阶段传最小但足够 RAG 使用的信息：
     * - requestId/sessionId/userMessageId：用于串联日志和调试；
     * - student：学生、课程、班级上下文，用于 Python 检索 Milvus 时做过滤；
     * - question：当前问题；
     * - memory：会话摘要和最近消息，后续支持多轮追问。
     *
     * <p>注意：Python 只消费这些字段做 RAG 检索和提示词构造，不负责学生鉴权。</p>
     */
    private StudentChatPythonStreamReq buildPythonStreamReq(ChatSessionEntity session,
                                                            ChatMessageEntity userMessage,
                                                            StudentEntity student,
                                                            String content) {
        StudentChatPythonStreamReq request = new StudentChatPythonStreamReq();
        request.setRequestId(IdUtil.fastSimpleUUID());
        request.setSessionId(session.getId());
        request.setUserMessageId(userMessage.getId());
        request.setStudent(buildStudentContext(student));

        StudentChatPythonStreamReq.QuestionContext question = new StudentChatPythonStreamReq.QuestionContext();
        question.setContent(content);
        question.setCreatedAt(String.valueOf(userMessage.getCreatedAt().getTime()));
        request.setQuestion(question);

        StudentChatPythonStreamReq.MemoryContext memory = new StudentChatPythonStreamReq.MemoryContext();
        memory.setSummary(StrUtil.blankToDefault(session.getSummary(), ""));
        memory.setRecentMessages(buildRecentMessages(session.getId(), session.getStudentId()));
        request.setMemory(memory);
        return request;
    }

    /**
     * 构造发给 Python 的学生上下文。
     *
     * <p>学生表里只有 classId，没有直接保存 courseId。
     * 所以这里通过班级查出课程，再把 courseId/courseName 一起传给 Python。
     * Python 侧会优先用 courseId 做知识库过滤，避免跨课程检索到不相关资料。</p>
     */
    private StudentChatPythonStreamReq.StudentContext buildStudentContext(StudentEntity student) {
        StudentChatPythonStreamReq.StudentContext context = new StudentChatPythonStreamReq.StudentContext();
        context.setStudentId(student.getId());
        context.setStudentCode(StrUtil.blankToDefault(student.getStudentCode(), ""));
        context.setStudentName(StrUtil.blankToDefault(student.getStudentName(), ""));
        context.setStudyMode(StrUtil.blankToDefault(student.getStudyMode(), ""));
        context.setClassId(student.getClassId());
        context.setClassName(StrUtil.blankToDefault(student.getClassName(), ""));

        if (student.getClassId() == null) {
            return context;
        }

        ClassEntity classInfo = classMapper.selectById(student.getClassId());
        if (classInfo == null) {
            return context;
        }

        if (StrUtil.isBlank(context.getClassName()) && classInfo.getClassTerm() != null) {
            context.setClassName(classInfo.getClassTerm() + "期班");
        }
        context.setCourseId(classInfo.getCourseId());
        context.setCourseName(StrUtil.blankToDefault(classInfo.getCourseName(), ""));

        if (classInfo.getCourseId() != null && StrUtil.isBlank(context.getCourseName())) {
            CourseEntity course = courseMapper.selectById(classInfo.getCourseId());
            if (course != null) {
                context.setCourseName(StrUtil.blankToDefault(course.getCourseName(), ""));
            }
        }
        return context;
    }

    private void triggerSummaryCompressionAsync(Long sessionId, Long studentId) {
        // 摘要是性能优化，不是本轮回答的必需步骤。
        // 放到异步里可以避免学生等模型二次调用完成后才看到 DONE。
        CompletableFuture.runAsync(() -> {
            try {
                maybeCompressSession(sessionId, studentId);
            } catch (Exception ex) {
                log.warn("学生问答会话摘要压缩失败: sessionId={}, studentId={}", sessionId, studentId, ex);
            }
        });
    }

    private void maybeCompressSession(Long sessionId, Long studentId) {
        RLock lock = redissonClient.getLock(RedisKeyConstants.studentAiSummaryCompressLockKey(sessionId));
        boolean locked;
        try {
            locked = lock.tryLock(0, SUMMARY_COMPRESS_LOCK_LEASE_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取会话摘要压缩锁失败");
        }
        if (!locked) {
            log.debug("学生问答会话摘要压缩已在执行，跳过本次任务: sessionId={}", sessionId);
            return;
        }

        try {
            ChatSessionEntity session = chatSessionMapper.selectByIdAndStudentId(sessionId, studentId);
            if (session == null) {
                return;
            }

            List<ChatMessageEntity> uncompressedMessages = chatMessageMapper.selectUncompressedBySessionIdAndStudentId(sessionId, studentId);
            int triggerUncompressedCount = getSummaryTriggerUncompressedCount();
            int keepRecentCount = getSummaryKeepRecentCount();
            // 未压缩消息不多时不触发，避免短对话每轮都调用一次摘要模型。
            if (uncompressedMessages.size() < triggerUncompressedCount) {
                return;
            }

            // 最近几条原文保留给下一轮追问使用，较早消息才合并进长期 summary。
            int compressEndIndex = uncompressedMessages.size() - keepRecentCount;
            if (compressEndIndex <= 0) {
                return;
            }

            List<ChatMessageEntity> messagesToCompress = uncompressedMessages.subList(0, compressEndIndex);
            ChatMessageEntity lastCompressedMessage = messagesToCompress.get(messagesToCompress.size() - 1);
            StudentChatPythonSummarizeRes summarizeRes = callPythonSummarize(session, messagesToCompress);

            String summary = StrUtil.blankToDefault(summarizeRes.getSummary(), "");
            if (StrUtil.isBlank(summary)) {
                return;
            }
            Long compressedUntilMessageId = summarizeRes.getCompressedUntilMessageId() == null
                    ? lastCompressedMessage.getId()
                    : summarizeRes.getCompressedUntilMessageId();
            Integer summaryTokenCount = summarizeRes.getSummaryTokenCount() == null
                    ? estimateTokenCount(summary)
                    : summarizeRes.getSummaryTokenCount();

            // 先更新 session.summary，再标记消息 compressed。
            // 后续构造 prompt 时会使用 summary + 未压缩 recent messages，避免重复塞入旧消息。
            chatSessionMapper.updateSummary(sessionId, studentId, summary, summaryTokenCount, compressedUntilMessageId, new Date());
            chatMessageMapper.markCompressedUntil(sessionId, studentId, compressedUntilMessageId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private StudentChatPythonSummarizeRes callPythonSummarize(ChatSessionEntity session, List<ChatMessageEntity> messages) {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled()) || !StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 未启用或未配置");
        }

        StudentChatPythonSummarizeReq request = new StudentChatPythonSummarizeReq();
        request.setSessionId(session.getId());
        request.setPreviousSummary(StrUtil.blankToDefault(session.getSummary(), ""));
        request.setMaxSummaryTokens(getSummaryMaxTokens());
        // 只发送本次需要压缩的旧消息；最近几条原文仍留在 Java 侧后续直接传给问答模型。
        request.setMessages(messages.stream().map(this::toSummarizeMessageContext).toList());

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(buildPythonStudentChatSummarizeUrl()))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(Duration.ofMillis(pythonAgentProperties.getTimeoutMs()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(request), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new BusinessException("Python 会话摘要接口返回异常：" + response.statusCode() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), StudentChatPythonSummarizeRes.class);
        } catch (IOException ex) {
            throw new BusinessException("Python 会话摘要接口调用失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Python 会话摘要接口调用已中断");
        }
    }

    private int getSummaryTriggerUncompressedCount() {
        return clamp(systemConfigService.get(SystemConfig.STUDENT_AI_SUMMARY_TRIGGER_UNCOMPRESSED_COUNT), 4, 100);
    }

    private int getSummaryKeepRecentCount() {
        return clamp(systemConfigService.get(SystemConfig.STUDENT_AI_SUMMARY_KEEP_RECENT_COUNT), 2, 30);
    }

    private int getSummaryMaxTokens() {
        return clamp(systemConfigService.get(SystemConfig.STUDENT_AI_SUMMARY_MAX_TOKENS), 100, 2000);
    }

    private int clamp(Integer value, int min, int max) {
        int actual = value == null ? min : value;
        return Math.max(min, Math.min(max, actual));
    }

    private StudentChatPythonSummarizeReq.ChatMessageContext toSummarizeMessageContext(ChatMessageEntity entity) {
        StudentChatPythonSummarizeReq.ChatMessageContext context = new StudentChatPythonSummarizeReq.ChatMessageContext();
        context.setId(entity.getId());
        context.setRole(entity.getRole());
        context.setContent(entity.getContent());
        context.setCreatedAt(String.valueOf(entity.getCreatedAt().getTime()));
        return context;
    }

    private List<StudentChatPythonStreamReq.ChatMessageContext> buildRecentMessages(Long sessionId, Long studentId) {
        // 只取未压缩消息。已压缩的历史内容已经进入 session.summary，避免 prompt 里重复出现。
        List<ChatMessageEntity> messages = chatMessageMapper.selectUncompressedBySessionIdAndStudentId(sessionId, studentId);
        int fromIndex = Math.max(0, messages.size() - 8);
        return messages.subList(fromIndex, messages.size()).stream()
                .map(this::toPythonMessageContext)
                .toList();
    }

    private StudentChatPythonStreamReq.ChatMessageContext toPythonMessageContext(ChatMessageEntity entity) {
        StudentChatPythonStreamReq.ChatMessageContext context = new StudentChatPythonStreamReq.ChatMessageContext();
        context.setId(entity.getId());
        context.setRole(entity.getRole());
        context.setContent(entity.getContent());
        context.setCreatedAt(String.valueOf(entity.getCreatedAt().getTime()));
        return context;
    }

    private String buildPythonStudentChatSummarizeUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getStudentChatSummarizePath())
                ? pythonAgentProperties.getStudentChatSummarizePath().trim()
                : "/api/student-chat/summarize";
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    private String buildPythonStudentChatStreamUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getStudentChatStreamPath())
                ? pythonAgentProperties.getStudentChatStreamPath().trim()
                : "/api/student-chat/stream";
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    @Override
    public void deleteSession(Long sessionId) {
        StudentEntity student = requireCurrentStudent();
        requireSession(sessionId, student.getId());
        chatSessionMapper.markDeleted(sessionId, student.getId(), new Date());
    }

    private StudentEntity requireCurrentStudent() {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || StrUtil.isBlank(accessContext.getStudentCode())) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }
        StudentEntity student = studentService.findByStudentCode(accessContext.getStudentCode());
        if (student == null) {
            throw new BusinessException(404, "学员信息不存在");
        }
        return student;
    }

    private ChatSessionEntity requireSession(Long sessionId, Long studentId) {
        if (sessionId == null) {
            throw new BusinessException("会话不存在");
        }
        ChatSessionEntity session = chatSessionMapper.selectByIdAndStudentId(sessionId, studentId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在或无权访问");
        }
        return session;
    }

    private ChatMessageEntity buildMessage(Long sessionId, Long studentId, String role, String content, String modelName, String finishReason, Date createdAt) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setStudentId(studentId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setContentType(CONTENT_TYPE_MARKDOWN);
        entity.setTokenCount(estimateTokenCount(content));
        entity.setRetrievalId(null);
        entity.setSourceRefsJson("[]");
        entity.setModelName(modelName);
        entity.setFinishReason(finishReason);
        entity.setCompressed(false);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private String normalizeTitle(String title) {
        String value = StrUtil.trim(title);
        if (StrUtil.isBlank(value)) {
            return "新对话";
        }
        return value.length() <= 80 ? value : value.substring(0, 80);
    }

    private String buildTitle(String content) {
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 24) {
            return compact;
        }
        return compact.substring(0, 24) + "...";
    }

    private Integer estimateTokenCount(String content) {
        if (StrUtil.isBlank(content)) {
            return 0;
        }
        return Math.max(1, content.trim().length() / 2);
    }

    private String buildMockAnswer(String question) {
        return """
                我已经收到你的问题：

                > %s

                当前第一版先完成了学生端问答的会话、消息保存和页面体验，还没有接入真实 RAG 知识库。下一步接入 Python Agent 后，我会基于你的课程资料和知识库进行检索，再用流式方式返回答案。
                """.formatted(question);
    }

    private List<String> splitChunks(String value, int chunkSize) {
        List<String> chunks = new java.util.ArrayList<>();
        for (int index = 0; index < value.length(); index += chunkSize) {
            chunks.add(value.substring(index, Math.min(index + chunkSize, value.length())));
        }
        return chunks;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("回答生成已中断");
        }
    }

    private StudentAiChatSessionRes toSessionRes(ChatSessionEntity entity) {
        StudentAiChatSessionRes res = new StudentAiChatSessionRes();
        res.setId(entity.getId());
        res.setTitle(entity.getTitle());
        res.setStatus(entity.getStatus());
        res.setMessageCount(entity.getMessageCount());
        res.setLastMessageAt(entity.getLastMessageAt());
        res.setSummary(entity.getSummary());
        res.setArchiveStatus(entity.getArchiveStatus());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }

    private StudentAiChatMessageRes toMessageRes(ChatMessageEntity entity) {
        StudentAiChatMessageRes res = new StudentAiChatMessageRes();
        res.setId(entity.getId());
        res.setSessionId(entity.getSessionId());
        res.setRole(entity.getRole());
        res.setContent(entity.getContent());
        res.setContentType(entity.getContentType());
        res.setTokenCount(entity.getTokenCount());
        res.setSourceRefsJson(entity.getSourceRefsJson());
        res.setModelName(entity.getModelName());
        res.setFinishReason(entity.getFinishReason());
        res.setCompressed(entity.getCompressed());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }
}
