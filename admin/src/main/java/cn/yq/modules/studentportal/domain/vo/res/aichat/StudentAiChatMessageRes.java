package cn.yq.modules.studentportal.domain.vo.res.aichat;

import lombok.Data;

import java.util.Date;

@Data
public class StudentAiChatMessageRes {

    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    private String contentType;

    private Integer tokenCount;

    private String sourceRefsJson;

    private String modelName;

    private String finishReason;

    private Boolean compressed;

    private Date createdAt;
}
