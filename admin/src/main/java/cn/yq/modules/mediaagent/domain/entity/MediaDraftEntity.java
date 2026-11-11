package cn.yq.modules.mediaagent.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 自媒体文字草稿。
 */
@Data
public class MediaDraftEntity {

    private Long id;

    private Long projectId;

    private String contentType;

    private String titleOptions;

    private String coverText;

    private String body;

    private String imagePagePlan;

    private String shotList;

    private String subtitles;

    private String hashtags;

    private Integer version;

    private String status;

    private Long createdBy;

    private Date createdAt;

    private Date updatedAt;
}
