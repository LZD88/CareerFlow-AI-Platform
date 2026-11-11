package cn.yq.modules.mediaagent.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 自媒体创作项目。
 */
@Data
public class MediaProjectEntity {

    private Long id;

    private String theme;

    private String topicJson;

    private String topicTitle;

    private String style;

    private String status;

    private Long createdBy;

    private Date createdAt;

    private Date updatedAt;
}
