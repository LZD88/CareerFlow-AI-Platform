package cn.yq.integration.yeepay.callback.service;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.payment.service.PaymentOrderService;
import com.alibaba.fastjson2.JSON;
import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class YeepayPaymentCallbackService {
    private final PaymentOrderService paymentOrderService;
    private final YeepayPaymentCallbackTxService transactions;

    public void handle(YopCallback callback) {
        if (callback == null || !callback.isValidSign()) throw new BusinessException(403, "支付通知验签失败");
        var data = JSON.parseObject(callback.getBizData());
        if (data == null || data.getString("orderId") == null) throw new BusinessException(400, "支付通知缺少订单号");
        var payment = paymentOrderService.findByPaymentOrderNo(data.getString("orderId"));
        if (payment == null) throw new BusinessException(404, "支付通知未匹配本地支付单");
        log.info("receive verified payment notification, paymentOrderNo={}, status={}", payment.getPaymentOrderNo(), data.getString("status"));
        transactions.persistCallbackResult(callback, callback.getBizData(), data.getString("uniqueOrderNo"), data.getString("status"), payment);
    }
}
