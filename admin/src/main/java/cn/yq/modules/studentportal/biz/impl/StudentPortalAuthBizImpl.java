package cn.yq.modules.studentportal.biz.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.enums.OrderStatusEnum;
import cn.yq.modules.sales.service.OrderService;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalAuthBiz;
import cn.yq.modules.studentportal.biz.support.StudentPortalBizSupport;
import cn.yq.modules.studentportal.domain.vo.req.StudentPortalLoginReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentPortalLoginRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentPortalAuthBizImpl implements StudentPortalAuthBiz {

    private final StudentService studentService;
    private final OrderService orderService;
    private final StudentPortalBizSupport support;

    @Override
    public StudentPortalLoginRes login(StudentPortalLoginReq request) {
        String studentCode = request.getStudentCode().trim();

        StudentEntity student = studentService.findByStudentCode(studentCode);
        if (student != null) {
            if (!student.getPassword().equals(request.getPassword())) {
                throw new BusinessException(401, "学号或密码错误");
            }
            StudentPortalLoginRes res = new StudentPortalLoginRes();
            res.setAction(StudentPortalBizSupport.ACTION_LOGIN_SUCCESS);
            res.setStudentId(student.getId());
            res.setStudentCode(student.getStudentCode());
            res.setStudentName(student.getStudentName());
            support.fillLoginSession(res, student.getId(), student.getStudentCode(), student.getStudentName(), StudentPortalBizSupport.SCOPE_FULL, null);
            return res;
        }

        OrderEntity order = orderService.findLatestByStudentCode(studentCode);
        if (order == null) {
            throw new BusinessException(404, "学员不存在");
        }
        if (OrderStatusEnum.UNPAID.getCode().equals(order.getOrderStatus())) {
            StudentPortalLoginRes res = new StudentPortalLoginRes();
            res.setAction(StudentPortalBizSupport.ACTION_PAY_REQUIRED);
            res.setPaymentPreview(support.toPaymentPreview(order));
            res.setStudentCode(order.getStudentCode());
            res.setStudentName(order.getStudentName());
            support.fillLoginSession(res, null, order.getStudentCode(), order.getStudentName(), StudentPortalBizSupport.SCOPE_PAY_ONLY, order.getId());
            return res;
        }
        if (OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            StudentPortalLoginRes res = new StudentPortalLoginRes();
            res.setAction(StudentPortalBizSupport.ACTION_PROFILE_REQUIRED);
            res.setStudentCode(order.getStudentCode());
            res.setStudentName(order.getStudentName());
            res.setEnrollmentDraft(support.toEnrollmentDraft(order));
            support.fillLoginSession(res, null, order.getStudentCode(), order.getStudentName(), StudentPortalBizSupport.SCOPE_PROFILE_ONLY, order.getId());
            return res;
        }
        throw new BusinessException(403, "当前学员账号尚未开通，请联系老师处理");
    }
}
