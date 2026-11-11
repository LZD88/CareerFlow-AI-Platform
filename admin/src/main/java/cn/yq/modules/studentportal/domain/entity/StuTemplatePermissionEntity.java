package cn.yq.modules.studentportal.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StuTemplatePermissionEntity {

    private Long id;

    private Long templateId;

    private Long permissionId;

    private Date createdAt;
}
