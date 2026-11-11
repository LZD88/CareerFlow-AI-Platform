package cn.yq.modules.payment;

import cn.yq.common.exception.BusinessException;
import cn.yq.integration.yeepay.callback.service.YeepayPaymentCallbackService;
import cn.yq.integration.yeepay.callback.service.YeepayPaymentCallbackTxService;
import cn.yq.modules.payment.service.PaymentOrderService;
import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentCallbackSecurityTest {
    @Test void invalidSignatureIsRejectedBeforeLookingUpAnyOrder() {
        var payments = mock(PaymentOrderService.class);
        var tx = mock(YeepayPaymentCallbackTxService.class);
        var callback = mock(YopCallback.class);
        when(callback.isValidSign()).thenReturn(false);
        assertThrows(BusinessException.class, () -> new YeepayPaymentCallbackService(payments,tx).handle(callback));
        verifyNoInteractions(payments,tx);
    }
    @Test void verifiedButUnknownOrderIsRejectedWithoutPersistence() {
        var payments = mock(PaymentOrderService.class);
        var tx = mock(YeepayPaymentCallbackTxService.class);
        var callback = mock(YopCallback.class);
        when(callback.isValidSign()).thenReturn(true);
        when(callback.getBizData()).thenReturn("{\"orderId\":\"UNKNOWN\",\"uniqueOrderNo\":\"OTHER\"}");
        assertThrows(BusinessException.class, () -> new YeepayPaymentCallbackService(payments,tx).handle(callback));
        verifyNoInteractions(tx);
    }
}
