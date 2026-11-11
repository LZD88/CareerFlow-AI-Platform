package cn.yq.modules.studentportal.domain.vo.req.aichat;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentChatPythonStreamReq {

    /**
     * 一次问答请求的唯一 ID。
     * Java 和 Python 都打这个 ID，后面排查一条流式请求会轻松很多。
     */
    private String requestId;

    /**
     * Java 侧会话 ID。
     * Python 不用拿它查库，只用于日志和返回事件关联。
     */
    private Long sessionId;

    /**
     * Java 已经保存好的用户消息 ID。
     * 后续如果 Python 返回 SOURCE / usage，Java 可以把结果挂回这条消息或本轮回答。
     */
    private Long userMessageId;

    /**
     * 当前登录学生上下文。
     *
     * <p>Python 不拿这些信息做鉴权，鉴权仍然在 Java 完成。
     * 这里传给 Python 的主要目的，是让 RAG 检索能按课程/班级过滤知识库。</p>
     */
    private StudentContext student = new StudentContext();

    private QuestionContext question;

    private MemoryContext memory = new MemoryContext();

    private GenerationContext generation = new GenerationContext();

    @Data
    public static class StudentContext {
        private Long studentId;
        private String studentCode = "";
        private String studentName = "";
        private String studyMode = "";
        private Long classId;
        private String className = "";
        private Long courseId;
        private String courseName = "";
    }

    @Data
    public static class QuestionContext {
        private String content;
        private String createdAt;
    }

    @Data
    public static class MemoryContext {
        /**
         * 会话长期摘要。
         * 当前阶段可以为空，后续上下文压缩完成后再传。
         */
        private String summary = "";

        /**
         * 最近几轮消息。
         * Python 用它理解“这个/那个/继续”等多轮追问，不需要反查 Java 数据库。
         */
        private List<ChatMessageContext> recentMessages = new ArrayList<>();
    }

    @Data
    public static class ChatMessageContext {
        private Long id;
        private String role;
        private String content;
        private String createdAt;
    }

    @Data
    public static class GenerationContext {
        private Boolean stream = Boolean.TRUE;
        private Integer maxTokens = 2000;
    }
}
