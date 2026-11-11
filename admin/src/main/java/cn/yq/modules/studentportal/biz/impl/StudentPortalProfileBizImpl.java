package cn.yq.modules.studentportal.biz.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalProfileBiz;
import cn.yq.modules.studentportal.biz.support.StudentPortalBizSupport;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.vo.res.StudentProfileRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentPortalProfileBizImpl implements StudentPortalProfileBiz {

    private final StudentService studentService;
    private final StudentPortalBizSupport support;

    @Override
    public StudentProfileRes profile() {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || accessContext.getStudentCode() == null || accessContext.getStudentCode().trim().isEmpty()) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }

        if (StudentPortalBizSupport.SCOPE_PROFILE_ONLY.equals(accessContext.getScope())) {
            if (accessContext.getOrderId() == null) {
                throw new BusinessException(403, "当前登录态不支持访问学员信息");
            }
            return support.toProfile(support.requireAuthorizedPaidOrder(accessContext.getOrderId()));
        }

        StudentEntity student = studentService.findByStudentCode(accessContext.getStudentCode());
        if (student == null) {
            throw new BusinessException(404, "学员信息不存在");
        }
        return support.toProfile(student);
    }
}
