package cn.yq.modules.studentportal.domain.vo.req.aichat;

import lombok.Data;

import java.util.List;

/**
 * Java 后端异步调用 Python Agent 生成会话摘要的请求体。
 */
@Data
public class StudentChatPythonSummarizeReq {

    private Long sessionId;
    private String previousSummary;
    private List<ChatMessageContext> messages;
    private Integer maxSummaryTokens;

    @Data
    public static class ChatMessageContext {
        private Long id;
        private String role;
        private String content;
        private String createdAt;
    }
}
