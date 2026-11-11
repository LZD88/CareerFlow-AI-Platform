package cn.yq.integration.yeepay.callback.service;

import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import org.springframework.transaction.annotation.Transactional;

public interface YeepayPaymentCallbackTxService {

    @Transactional(rollbackFor = Exception.class)
    void persistCallbackResult(YopCallback callback,
                               String bizData,
                               String uniqueOrderNo,
                               String status,
                               PaymentOrderEntity paymentOrder);
}
