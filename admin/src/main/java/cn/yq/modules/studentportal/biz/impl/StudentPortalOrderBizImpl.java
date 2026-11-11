package cn.yq.modules.studentportal.biz.impl;

import cn.yq.modules.payment.service.OrderCheckoutService;
import cn.yq.modules.payment.service.PaymentOrderService;
import cn.yq.modules.sales.service.OrderEnrollmentService;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalOrderBiz;
import cn.yq.modules.studentportal.biz.support.StudentPortalBizSupport;
import cn.yq.modules.studentportal.domain.vo.req.StudentEnrollmentSubmitReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentPaymentUnifiedOrderReq;
import cn.yq.modules.studentportal.domain.vo.res.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentPortalOrderBizImpl implements StudentPortalOrderBiz {
    private final OrderEnrollmentService enrollment;
    private final StudentService students;
    private final PaymentOrderService payments;
    private final OrderCheckoutService checkout;
    private final StudentPortalBizSupport support;

    @Override public StudentPaymentPreviewRes paymentPreview(Long id) {
        return support.toPaymentPreview(support.requireAuthorizedUnpaidOrder(id));
    }

    @Override public StudentPaymentCashierRes createCashierOrder(Long id, StudentPaymentUnifiedOrderReq request) {
        support.requireAuthorizedUnpaidOrder(id);
        var view = checkout.create(id); // 客户端编号和金额均不作为渠道下单参数。
        var result = new StudentPaymentCashierRes();
        result.setOrderId(id); result.setOrderNo(view.getOrderNo()); result.setPaymentOrderNo(view.getPaymentOrderNo());
        result.setCashierUrl(view.getCashierUrl());
        var payment = payments.findByPaymentOrderNo(view.getPaymentOrderNo());
        result.setPaymentOrderId(payment.getId()); result.setUniqueOrderNo(payment.getUniqueOrderNo());
        return result;
    }

    @Override public StudentPaymentReturnStatusRes paymentReturnStatus(Long id) {
        var order = support.requireAuthorizedOrder(id);
        return support.buildPaymentReturnStatus(order, payments.findLatestByOrderNo(order.getOrderNo()), students.findByStudentCode(order.getStudentCode()));
    }

    @Override public cn.yq.modules.payment.domain.vo.CheckoutView publicPaymentReturnStatus(Long id, String paymentNo) {
        return checkout.publicStatus(id, paymentNo);
    }

    @Override public StudentEnrollmentDraftRes enrollmentDraft(Long id) { return support.toEnrollmentDraft(support.requireAuthorizedPaidOrder(id)); }

    @Override public StudentPortalLoginRes submitEnrollment(Long id, StudentEnrollmentSubmitReq request) {
        var order = support.requireAuthorizedPaidOrder(id);
        Long studentId = enrollment.enroll(id, request);
        var result = new StudentPortalLoginRes();
        result.setAction(StudentPortalBizSupport.ACTION_LOGIN_SUCCESS); result.setStudentId(studentId);
        result.setStudentCode(order.getStudentCode()); result.setStudentName(order.getStudentName());
        support.fillLoginSession(result, studentId, order.getStudentCode(), order.getStudentName(), StudentPortalBizSupport.SCOPE_FULL, null);
        return result;
    }
}
