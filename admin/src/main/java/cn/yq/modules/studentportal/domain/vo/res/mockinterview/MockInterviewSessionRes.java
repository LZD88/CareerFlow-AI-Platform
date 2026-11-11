package cn.yq.modules.studentportal.domain.vo.res.mockinterview;

import lombok.Data;

import java.util.Date;

@Data
public class MockInterviewSessionRes {

    private Long id;

    private String title;

    private String interviewRole;

    private String status;

    private String volcSessionId;

    private String realtimeWsUrl;

    private String realtimeToken;

    private Long realtimeTokenExpiresIn;

    private Date realtimeTokenExpiresAt;

    private Date startedAt;

    private Date endedAt;

    private Date createdAt;

    private Date updatedAt;
}
