package cn.yq.modules.studentportal.service;

import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentBindPermissionTemplateReq;
import cn.yq.modules.student.domain.vo.res.StudentPermissionTemplateDetailRes;
import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;

public interface StudentPermissionTemplateBindingManageService {

    StudentPermissionTemplateDetailRes detail(Long studentId);

    void bind(Long studentId, StudentBindPermissionTemplateReq request);

    void unbind(Long studentId);

    StuPermissionTemplateEntity resolveEffectiveTemplate(StudentEntity student);

    void fillEffectiveTemplateSummary(StudentEntity student);
}
