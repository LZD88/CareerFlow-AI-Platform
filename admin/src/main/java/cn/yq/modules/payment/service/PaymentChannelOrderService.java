package cn.yq.modules.payment.service;

import cn.yq.modules.payment.domain.entity.PaymentChannelOrderEntity;

public interface PaymentChannelOrderService {

    void saveOrUpdate(PaymentChannelOrderEntity entity);

    PaymentChannelOrderEntity findLatestByPaymentOrderNo(String paymentOrderNo);
}
