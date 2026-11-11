package cn.yq.modules.studentportal.domain.vo.res.aichat;

import lombok.Data;

import java.util.Date;

@Data
public class StudentAiChatSessionRes {

    private Long id;

    private String title;

    private String status;

    private Integer messageCount;

    private Date lastMessageAt;

    private String summary;

    private String archiveStatus;

    private Date createdAt;

    private Date updatedAt;
}
