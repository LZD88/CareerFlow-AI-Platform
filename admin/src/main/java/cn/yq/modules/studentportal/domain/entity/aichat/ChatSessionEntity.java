package cn.yq.modules.studentportal.domain.entity.aichat;

import lombok.Data;

import java.util.Date;

@Data
public class ChatSessionEntity {

    private Long id;

    private Long studentId;

    private String studentCode;

    private String title;

    private String status;

    private Integer messageCount;

    private Date lastMessageAt;

    private String summary;

    private Integer summaryTokenCount;

    private Long compressedUntilMessageId;

    private String archiveStatus;

    private Long archiveId;

    private Date expireAt;

    private Date createdAt;

    private Date updatedAt;
}
