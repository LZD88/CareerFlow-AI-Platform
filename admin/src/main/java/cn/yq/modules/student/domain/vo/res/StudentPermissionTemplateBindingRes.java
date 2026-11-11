package cn.yq.modules.student.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class StudentPermissionTemplateBindingRes {

    private Long id;

    private Long studentId;

    private Long templateId;

    private String templateCode;

    private String templateName;

    private Date effectiveStartTime;

    private Date effectiveEndTime;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
