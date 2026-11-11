package cn.yq.modules.student.domain.entity;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

/**
 * 学员实体。
 */
@Data
public class StudentEntity {

    private Long id;

    private String studentCode;

    private String studentName;

    private String phone;

    @JsonIgnore
    private String password;

    private Integer graduationSession;

    private String school;

    private String education;

    private String studyMode;

    private Long classId;

    private String className;

    private Long productId;

    private String productName;

    private Long currentPermissionTemplateId;

    private String currentPermissionTemplateName;

    private String currentPermissionTemplateSource;

    private Date joinTime;

    private Date createdAt;

    private Date updatedAt;
}
