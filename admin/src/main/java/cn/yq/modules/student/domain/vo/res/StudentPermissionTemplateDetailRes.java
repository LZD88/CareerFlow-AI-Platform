package cn.yq.modules.student.domain.vo.res;

import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import lombok.Data;

@Data
public class StudentPermissionTemplateDetailRes {

    private Long studentId;

    private String studentCode;

    private String studentName;

    private StudentPermissionTemplateBindingRes manualBinding;

    private StuPermissionTemplateEntity autoMatchedTemplate;

    private StuPermissionTemplateEntity effectiveTemplate;

    private String source;
}
