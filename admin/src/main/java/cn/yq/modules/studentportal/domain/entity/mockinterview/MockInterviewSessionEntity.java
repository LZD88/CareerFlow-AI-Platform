package cn.yq.modules.studentportal.domain.entity.mockinterview;

import lombok.Data;

import java.util.Date;

@Data
public class MockInterviewSessionEntity {

    private Long id;

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String title;

    private String interviewRole;

    private String status;

    private String volcSessionId;

    private String realtimeToken;

    private Date realtimeTokenExpiresAt;

    private String transcriptText;

    private String failReason;

    private Date startedAt;

    private Date endedAt;

    private Date createdAt;

    private Date updatedAt;
}
