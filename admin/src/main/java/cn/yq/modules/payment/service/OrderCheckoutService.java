package cn.yq.modules.payment.service;

import cn.yq.common.exception.BusinessException;
import cn.yq.common.redis.RedisKeyConstants;
import cn.yq.config.YeepayProperties;
import cn.yq.config.YeepayDynamicProperties;
import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderReq;
import cn.yq.integration.yeepay.service.YeepayCashierService;
import cn.yq.integration.yeepay.service.YeepayReadiness;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.domain.vo.CheckoutView;
import cn.yq.modules.payment.mapper.PaymentOrderMapper;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {
    private final OrderMapper orders;
    private final PaymentOrderMapper payments;
    private final YeepayProperties properties;
    private final YeepayDynamicProperties dynamic;
    private final YeepayReadiness readiness;
    private final YeepayCashierService cashier;
    private final RedissonClient redisson;
    private final TransactionTemplate transactions;

    public CheckoutView preview(Long id) {
        OrderEntity order = requireOrder(id);
        PaymentOrderEntity latest = payments.selectLatestByOrderNo(order.getOrderNo());
        return view(order, latest != null && "YEEPAY".equals(latest.getPaymentChannel()) ? latest : null, true);
    }

    public CheckoutView publicStatus(Long id, String paymentOrderNo) {
        // 随机支付单号作为只读查询凭证，错误查询统一返回 404。
        PaymentOrderEntity payment = payments.selectByPaymentOrderNo(paymentOrderNo);
        OrderEntity order = orders.selectById(id);
        if (payment == null || order == null || !order.getOrderNo().equals(payment.getOrderNo()))
            throw new BusinessException(404, "未找到付款记录，请检查支付返回链接");
        return view(order, payment, false);
    }

    public CheckoutView create(Long id) {
        var initial = requireOrder(id);
        var lock = redisson.getLock(RedisKeyConstants.studentPaymentCreateLockKey(initial.getOrderNo()));
        boolean locked = false;
        try {
            // 使用 watchdog 续租；只覆盖发起请求，返回后解锁，回调不解其他线程的锁。
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) throw new BusinessException(409, "正在生成收银台，请稍后刷新");
            readiness.requireReady();
            PaymentOrderEntity payment = transactions.execute(tx -> reserve(id));
            if (payment.getCashierUrl() != null) return view(requireOrder(id), payment, true);
            try {
                var result = cashier.unifiedOrder(YeepayUnifiedOrderReq.builder()
                        .localPaymentOrderId(payment.getId()).businessOrderId(id)
                        .paymentOrderNo(payment.getPaymentOrderNo()).businessOrderNo(payment.getOrderNo())
                        .orderAmount(payment.getPaymentAmount()).goodsName(payment.getProductName())
                        .studentCode(payment.getStudentCode()).studentName(payment.getStudentName()).phone(payment.getPhone())
                        .expiresAt(payment.getExpiresAt()).build());
                payment.setUniqueOrderNo(result.getUniqueOrderNo());
                payment.setCashierUrl(result.getCashierUrl());
                payment.setPaymentStatus("UNIFIED_ORDER_CREATED");
                payment.setUpdatedAt(new Date());
                // 条件更新，防止下单响应晚于成功回调时把成功状态覆盖。
                payments.updateUnifiedOrderResult(payment);
                return view(requireOrder(id), payments.selectByPaymentOrderNo(payment.getPaymentOrderNo()), true);
            } catch (RuntimeException ex) {
                // 网络异常无法证明渠道没有建单，保留同一单等待确认，禁止再次扣款。
                payments.recordUncertain(payment.getId(), "渠道下单结果待确认，请核对易宝商户后台或等待支付通知");
                throw new BusinessException(502, "渠道下单结果待确认，请刷新状态并核对易宝商户后台，勿重复付款");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "收银台繁忙，请稍后重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private PaymentOrderEntity reserve(Long id) {
        if (orders.lockById(id) == null) throw new BusinessException(404, "订单不存在");
        OrderEntity order = requireOrder(id);
        if (!("UNPAID".equals(order.getOrderStatus()) || "PARTIAL_PAID".equals(order.getOrderStatus()))
                || remaining(order).signum() <= 0) throw new BusinessException(409, "该订单无需收款或已关闭");
        PaymentOrderEntity active = payments.selectOpenByOrderNo(order.getOrderNo());
        if (active != null) {
            if (active.getCashierUrl() != null && active.getExpiresAt() != null && active.getExpiresAt().after(new Date())
                    && active.getPaymentAmount().compareTo(remaining(order)) == 0) return active;
            throw new BusinessException(409, "已有待确认的支付单，请先核对原支付结果，避免重复扣款");
        }
        var payment = new PaymentOrderEntity();
        payment.setPaymentOrderNo("ZF" + UUID.randomUUID().toString().replace("-", ""));
        payment.setOrderNo(order.getOrderNo());
        payment.setStudentCode(order.getStudentCode());
        payment.setStudentName(order.getStudentName());
        payment.setPhone(order.getPhone());
        payment.setProductName(order.getProductName());
        payment.setPaymentAmount(remaining(order));
        payment.setPaymentChannel("YEEPAY");
        payment.setPaymentStatus("PROCESSING");
        payment.setMerchantNo(properties.getMerchantNo().trim());
        payment.setExpiresAt(Date.from(Instant.now().plus(dynamic.getExpireMinutes(), ChronoUnit.MINUTES)));
        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());
        payments.insert(payment);
        return payment;
    }

    private CheckoutView view(OrderEntity order, PaymentOrderEntity payment, boolean admin) {
        var res = new CheckoutView();
        res.setOrderId(order.getId()); res.setOrderNo(order.getOrderNo()); res.setOrderStatus(order.getOrderStatus());
        res.setReceivableAmount(order.getReceivableAmount()); res.setPaidAmount(zero(order.getPaidAmount()));
        res.setRemainingAmount(remaining(order));
        if (admin) {
            res.setMissingConfig(readiness.missing()); res.setReady(res.getMissingConfig().isEmpty());
            res.setSandbox(Boolean.TRUE.equals(dynamic.getUseSandbox()));
        }
        if (payment != null) {
            res.setPaymentOrderNo(payment.getPaymentOrderNo()); res.setPaymentStatus(payment.getPaymentStatus());
            res.setPaymentAmount(payment.getPaymentAmount()); res.setExpiresAt(payment.getExpiresAt());
            res.setExpired(payment.getExpiresAt() != null && !payment.getExpiresAt().after(new Date()));
            if (admin && !res.isExpired() && "UNIFIED_ORDER_CREATED".equals(payment.getPaymentStatus()))
                res.setCashierUrl(payment.getCashierUrl());
        }
        if ("PAID".equals(order.getOrderStatus())) res.setMessage("付款已完成，报名订单已更新，可由工作人员办理入班");
        else if ("PAY_SUCCESS".equals(res.getPaymentStatus())) res.setMessage("本次付款已到账，请联系工作人员核对订单");
        else if ("CLOSED".equals(order.getOrderStatus())) res.setMessage("报名订单已关闭，如已扣款请联系工作人员核实");
        else if ("FAILED".equals(res.getPaymentStatus()) || "CLOSED".equals(res.getPaymentStatus())) res.setMessage("本次付款未完成，请联系工作人员核实后重新发起");
        else if (res.isExpired()) res.setMessage("收银台已过期，支付结果仍待渠道确认，请勿重复付款");
        else res.setMessage("等待支付结果通知；已付款时请稍后刷新，勿重复付款");
        return res;
    }

    private OrderEntity requireOrder(Long id) {
        var order = orders.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        return order;
    }
    private BigDecimal remaining(OrderEntity order) { return zero(order.getReceivableAmount()).subtract(zero(order.getPaidAmount())).max(BigDecimal.ZERO); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
