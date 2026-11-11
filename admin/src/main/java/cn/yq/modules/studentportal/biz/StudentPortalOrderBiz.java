package cn.yq.modules.studentportal.biz;

import cn.yq.modules.studentportal.domain.vo.req.StudentEnrollmentSubmitReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentPaymentUnifiedOrderReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentEnrollmentDraftRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentCashierRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentPreviewRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentReturnStatusRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPortalLoginRes;

public interface StudentPortalOrderBiz {

    StudentPaymentPreviewRes paymentPreview(Long orderId);

    StudentPaymentCashierRes createCashierOrder(Long orderId, StudentPaymentUnifiedOrderReq request);

    StudentPaymentReturnStatusRes paymentReturnStatus(Long orderId);

    cn.yq.modules.payment.domain.vo.CheckoutView publicPaymentReturnStatus(Long orderId, String paymentOrderNo);

    StudentEnrollmentDraftRes enrollmentDraft(Long orderId);

    StudentPortalLoginRes submitEnrollment(Long orderId, StudentEnrollmentSubmitReq request);
}
