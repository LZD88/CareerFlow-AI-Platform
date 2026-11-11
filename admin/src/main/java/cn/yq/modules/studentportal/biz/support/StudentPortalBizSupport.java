package cn.yq.modules.studentportal.biz.support;

import cn.hutool.core.util.IdUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.AuthProperties;
import cn.yq.config.SignProperties;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.enums.PaymentOrderStatusEnum;
import cn.yq.modules.payment.service.PaymentOrderService;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.enums.OrderStatusEnum;
import cn.yq.modules.sales.service.OrderService;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.StudentPortalTokenClaims;
import cn.yq.modules.studentportal.domain.vo.res.StudentEnrollmentDraftRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentPreviewRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPaymentReturnStatusRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentPortalLoginRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentProfileRes;
import cn.yq.modules.studentportal.util.StudentPortalJwtTokenUtil;
import cn.yq.modules.studentportal.util.StudentPortalSignKeyRedisUtil;
import cn.yq.modules.studentportal.service.StudentPortalPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class StudentPortalBizSupport {

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_PAY_REQUIRED = "PAY_REQUIRED";
    public static final String ACTION_PAYING = "PAYING";
    public static final String ACTION_PROFILE_REQUIRED = "PROFILE_REQUIRED";
    public static final String SCOPE_FULL = "FULL";
    public static final String SCOPE_PAY_ONLY = "PAY_ONLY";
    public static final String SCOPE_PROFILE_ONLY = "PROFILE_ONLY";

    private final OrderService orderService;
    private final PaymentOrderService paymentOrderService;
    private final StudentPortalJwtTokenUtil studentPortalJwtTokenUtil;
    private final StudentPortalSignKeyRedisUtil studentPortalSignKeyRedisUtil;
    private final StudentService studentService;
    private final StudentPortalPermissionService studentPortalPermissionService;
    private final AuthProperties authProperties;
    private final SignProperties signProperties;

    public void fillLoginSession(StudentPortalLoginRes res,
                                 Long studentId,
                                 String studentCode,
                                 String studentName,
                                 String scope,
                                 Long orderId) {
        String signSecret = IdUtil.fastSimpleUUID();
        studentPortalSignKeyRedisUtil.cacheStudentSecret(studentCode, signSecret, signProperties.getSecretExpireSeconds());
        res.setToken(studentPortalJwtTokenUtil.createToken(StudentPortalTokenClaims.builder()
                .studentId(studentId)
                .studentCode(studentCode)
                .studentName(studentName)
                .scope(scope)
                .orderId(orderId)
                .build()));
        res.setExpiresIn(authProperties.getExpireSeconds());
        res.setSignSecret(signSecret);
        res.setSignSecretExpiresIn(signProperties.getSecretExpireSeconds());
        res.setScope(scope);
        res.setPermissions(Collections.emptyList());
        if (SCOPE_FULL.equals(scope) && studentId != null) {
            StudentEntity student = studentService.findByStudentCode(studentCode);
            if (student != null) {
                res.setPermissions(studentPortalPermissionService.listPermissionCodes(student));
            }
        }
    }

    public StudentPaymentPreviewRes toPaymentPreview(OrderEntity order) {
        StudentPaymentPreviewRes res = new StudentPaymentPreviewRes();
        res.setOrderId(order.getId());
        res.setOrderNo(order.getOrderNo());
        res.setStudentCode(order.getStudentCode());
        res.setStudentName(order.getStudentName());
        res.setProductName(order.getProductName());
        res.setReceivableAmount(order.getReceivableAmount());
        res.setPaidAmount(order.getPaidAmount());
        res.setRemainingAmount(order.getReceivableAmount().subtract(order.getPaidAmount() == null ? java.math.BigDecimal.ZERO : order.getPaidAmount()).max(java.math.BigDecimal.ZERO));
        res.setPaymentOrderNo(paymentOrderService.generatePaymentOrderNo());
        return res;
    }

    public StudentEnrollmentDraftRes toEnrollmentDraft(OrderEntity order) {
        StudentEnrollmentDraftRes res = new StudentEnrollmentDraftRes();
        res.setOrderId(order.getId());
        res.setOrderNo(order.getOrderNo());
        res.setStudentCode(order.getStudentCode());
        res.setStudentName(order.getStudentName());
        res.setPhone(order.getPhone());
        res.setProductId(order.getProductId());
        res.setProductName(order.getProductName());
        res.setStudyMode(order.getProductStudyMode());
        res.setClassId(order.getClassId());
        res.setClassTerm(order.getClassTerm());
        res.setJoinTime(new Date());
        return res;
    }

    public StudentProfileRes toProfile(StudentEntity student) {
        StudentProfileRes res = new StudentProfileRes();
        res.setStudentId(student.getId());
        res.setStudentCode(student.getStudentCode());
        res.setStudentName(student.getStudentName());
        res.setPhone(student.getPhone());
        res.setGraduationSession(student.getGraduationSession());
        res.setSchool(student.getSchool());
        res.setEducation(student.getEducation());
        res.setStudyMode(student.getStudyMode());
        res.setClassId(student.getClassId());
        res.setClassName(student.getClassName());
        res.setProductId(student.getProductId());
        res.setProductName(student.getProductName());
        res.setJoinTime(student.getJoinTime());
        return res;
    }

    public StudentProfileRes toProfile(OrderEntity order) {
        StudentProfileRes res = new StudentProfileRes();
        res.setStudentCode(order.getStudentCode());
        res.setStudentName(order.getStudentName());
        res.setPhone(order.getPhone());
        res.setStudyMode(order.getProductStudyMode());
        res.setClassId(order.getClassId());
        res.setClassName(order.getClassTerm() == null ? null : "第" + order.getClassTerm() + "期");
        res.setProductId(order.getProductId());
        res.setProductName(order.getProductName());
        return res;
    }

    public StudentPaymentReturnStatusRes buildPaymentReturnStatus(OrderEntity order,
                                                                  PaymentOrderEntity paymentOrder,
                                                                  StudentEntity student) {
        StudentPaymentReturnStatusRes res = new StudentPaymentReturnStatusRes();
        res.setOrderId(order.getId());
        res.setOrderNo(order.getOrderNo());
        res.setOrderStatus(order.getOrderStatus());
        res.setPaymentOrderNo(paymentOrder != null ? paymentOrder.getPaymentOrderNo() : null);
        res.setPaymentStatus(paymentOrder != null ? paymentOrder.getPaymentStatus() : null);
        res.setStudentCode(order.getStudentCode());
        res.setStudentName(order.getStudentName());

        if (OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            if (student != null) {
                res.setAction(ACTION_LOGIN_SUCCESS);
                res.setStudentId(student.getId());
                fillLoginSession(res, student.getId(), student.getStudentCode(), student.getStudentName(), SCOPE_FULL, null);
                res.setMessage("支付已完成，正在进入学习平台");
                return res;
            }
            res.setAction(ACTION_PROFILE_REQUIRED);
            res.setEnrollmentDraft(toEnrollmentDraft(order));
            fillLoginSession(res, null, order.getStudentCode(), order.getStudentName(), SCOPE_PROFILE_ONLY, order.getId());
            res.setMessage("支付已完成，请先补全入学资料");
            return res;
        }

        if (paymentOrder != null && isCashierStillWaiting(paymentOrder.getPaymentStatus())) {
            res.setAction(ACTION_PAYING);
            res.setMessage("正在确认支付结果，请稍候...");
            return res;
        }

        if (OrderStatusEnum.UNPAID.getCode().equals(order.getOrderStatus())) {
            res.setAction(ACTION_PAY_REQUIRED);
            res.setPaymentPreview(toPaymentPreview(order));
            fillLoginSession(res, null, order.getStudentCode(), order.getStudentName(), SCOPE_PAY_ONLY, order.getId());
            res.setMessage("暂未查询到支付成功结果，你可以返回支付页继续完成支付");
            return res;
        }

        throw new BusinessException(400, "当前订单状态不支持继续支付");
    }

    public OrderEntity requireUnpaidOrder(Long orderId) {
        OrderEntity order = orderService.detail(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!OrderStatusEnum.UNPAID.getCode().equals(order.getOrderStatus()) && !OrderStatusEnum.PARTIAL_PAID.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单不是待支付状态");
        }
        return order;
    }

    public OrderEntity requireAuthorizedOrder(Long orderId) {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || accessContext.getStudentCode() == null || accessContext.getStudentCode().trim().isEmpty()) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }
        OrderEntity order = orderService.detail(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!accessContext.getStudentCode().equals(order.getStudentCode())) {
            throw new BusinessException(403, "无权访问该订单");
        }
        if ((SCOPE_PAY_ONLY.equals(accessContext.getScope()) || SCOPE_PROFILE_ONLY.equals(accessContext.getScope()))
                && !order.getId().equals(accessContext.getOrderId())) {
            throw new BusinessException(403, "当前登录态仅允许访问指定订单");
        }
        return order;
    }

    public OrderEntity requireAuthorizedUnpaidOrder(Long orderId) {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || accessContext.getStudentCode() == null || accessContext.getStudentCode().trim().isEmpty()) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }
        OrderEntity order = requireUnpaidOrder(orderId);
        if (!accessContext.getStudentCode().equals(order.getStudentCode())) {
            throw new BusinessException(403, "无权访问该订单");
        }
        if (SCOPE_PAY_ONLY.equals(accessContext.getScope()) && !order.getId().equals(accessContext.getOrderId())) {
            throw new BusinessException(403, "当前登录态仅允许访问指定待支付订单");
        }
        return order;
    }

    public OrderEntity requireAuthorizedPaidOrder(Long orderId) {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        OrderEntity order = requireAuthorizedOrder(orderId);
        if (!OrderStatusEnum.PAID.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException(400, "当前订单未支付完成");
        }
        if (SCOPE_PROFILE_ONLY.equals(accessContext.getScope()) && !order.getId().equals(accessContext.getOrderId())) {
            throw new BusinessException(403, "当前登录态仅允许填写指定订单的入学资料");
        }
        return order;
    }

    private boolean isCashierStillWaiting(String paymentStatus) {
        return PaymentOrderStatusEnum.PROCESSING.getCode().equals(paymentStatus)
                || PaymentOrderStatusEnum.UNIFIED_ORDER_CREATED.getCode().equals(paymentStatus);
    }
}
