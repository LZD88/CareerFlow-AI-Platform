package cn.yq.integration.yeepay.callback.service.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.config.YeepayProperties;
import cn.yq.integration.yeepay.callback.service.YeepayPaymentCallbackTxService;
import cn.yq.modules.payment.domain.entity.PaymentChannelOrderEntity;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.mapper.PaymentOrderMapper;
import cn.yq.modules.payment.service.PaymentChannelOrderService;
import cn.yq.modules.sales.mapper.OrderMapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeepayPaymentCallbackTxServiceImpl implements YeepayPaymentCallbackTxService {
    private final PaymentOrderMapper payments;
    private final PaymentChannelOrderService channels;
    private final OrderMapper orders;
    private final YeepayProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void persistCallbackResult(YopCallback callback, String bizData, String uniqueOrderNo, String status, PaymentOrderEntity original) {
        if (callback == null || !callback.isValidSign()) throw new BusinessException(403, "支付通知验签失败");
        if (original == null) throw new BusinessException(404, "支付单不存在");
        // 所有支付业务先锁报名订单再锁支付单，串行化多次支付及重复通知。
        if (orders.lockByOrderNo(original.getOrderNo()) == null) throw new BusinessException(404, "报名订单不存在");
        var payment = payments.lockByPaymentOrderNo(original.getPaymentOrderNo());
        var data = JSON.parseObject(bizData);
        validate(payment, data, uniqueOrderNo, status);
        if ("PAY_SUCCESS".equals(payment.getPaymentStatus())) return;
        boolean success = "SUCCESS".equalsIgnoreCase(status);
        boolean failed = "FAIL".equalsIgnoreCase(status);
        if (!success && !failed) return;
        channels.saveOrUpdate(channel(callback, data, payment));
        payment.setUniqueOrderNo(uniqueOrderNo);
        payment.setPaymentStatus(success ? "PAY_SUCCESS" : "FAILED");
        String reason = data.getString("failReason");
        payment.setLastErrorMsg(success ? null : reason == null ? data.getString("failCode") : reason.substring(0, Math.min(reason.length(), 255)));
        payment.setUpdatedAt(new Date());
        if (payments.updateCallbackResult(payment) != 1) throw new BusinessException(409, "支付结果更新冲突，请重试通知");
        if (success) {
            var order = orders.selectByOrderNo(payment.getOrderNo());
            // 只在本支付单首次成功时累加，兼容既往线下实收和部分支付。
            BigDecimal paid = (order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount()).add(payment.getPaymentAmount());
            order.setPaidAmount(paid);
            if (!"CLOSED".equals(order.getOrderStatus()))
                order.setOrderStatus(paid.compareTo(order.getReceivableAmount()) >= 0 ? "PAID" : "PARTIAL_PAID");
            order.setUpdatedAt(new Date());
            if (orders.updateById(order) != 1) throw new BusinessException(409, "报名订单更新冲突");
            if (paid.compareTo(order.getReceivableAmount()) > 0 || "CLOSED".equals(order.getOrderStatus()))
                log.warn("Payment requires reconciliation, orderNo={}, paymentOrderNo={}", order.getOrderNo(), payment.getPaymentOrderNo());
        }
    }

    private void validate(PaymentOrderEntity payment, JSONObject data, String uniqueNo, String status) {
        if (payment == null || data == null || !"YEEPAY".equals(payment.getPaymentChannel())) throw new BusinessException(400, "支付通知订单无效");
        String merchant = payment.getMerchantNo() == null ? properties.getMerchantNo() : payment.getMerchantNo();
        if (merchant == null || merchant.isBlank() || !merchant.equals(data.getString("merchantNo"))) throw new BusinessException(400, "支付通知商户不匹配");
        if (!payment.getPaymentOrderNo().equals(data.getString("orderId")) || uniqueNo == null || uniqueNo.isBlank()
                || !Objects.equals(uniqueNo, data.getString("uniqueOrderNo")) || !Objects.equals(status, data.getString("status"))
                || (payment.getUniqueOrderNo() != null && !payment.getUniqueOrderNo().equals(uniqueNo)))
            throw new BusinessException(400, "支付通知订单号不匹配");
        BigDecimal amount;
        try { amount = data.getBigDecimal("orderAmount"); } catch (Exception ex) { throw new BusinessException(400, "支付通知金额无效"); }
        if (amount == null || amount.compareTo(payment.getPaymentAmount()) != 0) throw new BusinessException(400, "支付通知金额不匹配");
    }

    private PaymentChannelOrderEntity channel(YopCallback callback, JSONObject data, PaymentOrderEntity payment) {
        var entity = new PaymentChannelOrderEntity();
        entity.setPaymentOrderNo(payment.getPaymentOrderNo()); entity.setUniqueOrderNo(data.getString("uniqueOrderNo"));
        entity.setMerchantNo(data.getString("merchantNo")); entity.setOrderId(data.getString("orderId"));
        entity.setChannelOrderId(data.getString("channelOrderId")); entity.setOrderAmount(data.getBigDecimal("orderAmount"));
        entity.setPayAmount(data.getBigDecimal("payAmount")); entity.setStatus(data.getString("status"));
        entity.setPaySuccessDate(data.getString("paySuccessDate")); entity.setChannel(data.getString("channel"));
        entity.setPayerInfo(JSON.toJSONString(data.get("payerInfo"))); entity.setFailCode(data.getString("failCode"));
        entity.setFailReason(data.getString("failReason")); entity.setChannelTrxId(data.getString("channelTrxId"));
        entity.setBankOrderId(data.getString("bankOrderId")); entity.setCallbackType(callback.getType());
        entity.setCallbackBody(callback.getBizData()); entity.setCallbackTime(new Date());
        return entity;
    }
}
