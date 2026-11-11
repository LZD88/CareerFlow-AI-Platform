package cn.yq.modules.studentportal.domain.entity.aichat;

import lombok.Data;

import java.util.Date;

@Data
public class ChatMessageEntity {

    private Long id;

    private Long sessionId;

    private Long studentId;

    private String role;

    private String content;

    private String contentType;

    private Integer tokenCount;

    private Long retrievalId;

    private String sourceRefsJson;

    private String modelName;

    private String finishReason;

    private Boolean compressed;

    private Date createdAt;
}
