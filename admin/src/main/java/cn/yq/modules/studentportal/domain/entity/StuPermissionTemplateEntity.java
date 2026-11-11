package cn.yq.modules.studentportal.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StuPermissionTemplateEntity {

    private Long id;

    private String templateCode;

    private String templateName;

    private String studyMode;

    private Long productId;

    private Long classId;

    private String description;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
