package cn.yq.modules.payment.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.domain.vo.req.PaymentOrderQueryReq;
import cn.yq.modules.sales.domain.entity.OrderEntity;

public interface PaymentOrderService {

    PageResult<PaymentOrderEntity> page(PaymentOrderQueryReq request);

    String generatePaymentOrderNo();

    PaymentOrderEntity createIfAbsent(OrderEntity order, String paymentOrderNo);

    PaymentOrderEntity findByPaymentOrderNo(String paymentOrderNo);

    PaymentOrderEntity findLatestByOrderNo(String orderNo);

    PaymentOrderEntity findByUniqueOrderNo(String uniqueOrderNo);

    boolean markProcessing(Long id);

    void saveUnifiedOrderResult(PaymentOrderEntity entity);

    void markFailed(Long id, String lastErrorMsg);

    void handleCallbackResult(PaymentOrderEntity entity);

    boolean closeIfUnifiedOrderCreated(Long id, String lastErrorMsg);
}
