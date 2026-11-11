package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentportal.biz.StudentPortalOrderBiz;
import cn.yq.modules.studentportal.domain.vo.req.StudentEnrollmentSubmitReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentPaymentUnifiedOrderReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentEnrollmentDraftRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentCashierRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentPreviewRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentReturnStatusRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPortalLoginRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端订单与支付入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api")
public class StudentPortalOrderController {

    private final StudentPortalOrderBiz studentPortalOrderBiz;

    /**
     * 查询待支付订单预览信息。
     */
    @GetMapping("/orders/{orderId}/payment-preview")
    public ApiResponse<StudentPaymentPreviewRes> paymentPreview(@PathVariable Long orderId) {
        return ApiResponse.success(studentPortalOrderBiz.paymentPreview(orderId));
    }

    /**
     * 创建收银台订单，返回支付链接。
     */
    @PostMapping("/orders/{orderId}/cashier-unified-order")
    public ApiResponse<StudentPaymentCashierRes> createCashierOrder(@PathVariable Long orderId,
                                                                    @RequestBody @Valid StudentPaymentUnifiedOrderReq request) {
        return ApiResponse.success("下单成功", studentPortalOrderBiz.createCashierOrder(orderId, request));
    }

    /**
     * 支付回跳页轮询订单当前支付状态。
     */
    @GetMapping("/orders/{orderId}/payment-return-status")
    public ApiResponse<StudentPaymentReturnStatusRes> paymentReturnStatus(@PathVariable Long orderId) {
        return ApiResponse.success(studentPortalOrderBiz.paymentReturnStatus(orderId));
    }

    /**
     * 支付平台回跳后公开查询支付状态，不要求先登录。
     */
    @GetMapping("/public/orders/{orderId}/payment-return-status")
    public ApiResponse<cn.yq.modules.payment.domain.vo.CheckoutView> publicPaymentReturnStatus(@PathVariable Long orderId,
                                                                                @RequestParam String paymentOrderNo) {
        return ApiResponse.success(studentPortalOrderBiz.publicPaymentReturnStatus(orderId, paymentOrderNo));
    }

    /**
     * 查询已支付订单对应的入学资料草稿。
     */
    @GetMapping("/orders/{orderId}/enrollment-draft")
    public ApiResponse<StudentEnrollmentDraftRes> enrollmentDraft(@PathVariable Long orderId) {
        return ApiResponse.success(studentPortalOrderBiz.enrollmentDraft(orderId));
    }

    /**
     * 提交入学资料并创建学生档案。
     */
    @PostMapping("/orders/{orderId}/enrollment")
    public ApiResponse<StudentPortalLoginRes> submitEnrollment(@PathVariable Long orderId,
                                                               @RequestBody @Valid StudentEnrollmentSubmitReq request) {
        return ApiResponse.success("入学资料提交成功", studentPortalOrderBiz.submitEnrollment(orderId, request));
    }
}
