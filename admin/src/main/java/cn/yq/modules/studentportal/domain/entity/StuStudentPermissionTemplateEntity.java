package cn.yq.modules.studentportal.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StuStudentPermissionTemplateEntity {

    private Long id;

    private Long studentId;

    private Long templateId;

    private Date effectiveStartTime;

    private Date effectiveEndTime;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
