package cn.yq.modules.payment.service.impl;

import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.enums.PaymentOrderStatusEnum;
import cn.yq.modules.payment.service.PaymentDelayCloseTxService;
import cn.yq.modules.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDelayCloseTxServiceImpl implements PaymentDelayCloseTxService {

    private static final String AUTO_CLOSE_REASON = "收银台超时未支付，系统自动关闭";

    private final PaymentOrderService paymentOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeIfStillWaiting(String paymentOrderNo) {
        PaymentOrderEntity paymentOrder = paymentOrderService.findByPaymentOrderNo(paymentOrderNo);
        if (paymentOrder == null) {
            return;
        }
        if (!PaymentOrderStatusEnum.UNIFIED_ORDER_CREATED.getCode().equals(paymentOrder.getPaymentStatus())) {
            return;
        }

        boolean paymentClosed = paymentOrderService.closeIfUnifiedOrderCreated(paymentOrder.getId(), AUTO_CLOSE_REASON);
        if (!paymentClosed) {
            return;
        }

        log.info("payment delay close executed, paymentOrderNo={}, orderNo={}",
                paymentOrderNo, paymentOrder.getOrderNo());
    }
}
