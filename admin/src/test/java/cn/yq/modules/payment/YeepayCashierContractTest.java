package cn.yq.modules.payment;

import cn.yq.common.exception.BusinessException;
import cn.yq.config.YeepayProperties;
import cn.yq.config.YeepayDynamicProperties;
import cn.yq.integration.yeepay.domain.*;
import cn.yq.integration.yeepay.service.YeepayGatewayService;
import cn.yq.integration.yeepay.service.impl.YeepayCashierServiceImpl;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YeepayCashierContractTest {
    @Test void sendsServerAmountAndPaymentIdentifiersToExistingYeepayAdapter() {
        var dynamic = mock(YeepayDynamicProperties.class);
        var properties = new YeepayProperties(); properties.setMerchantNo("TEST");
        when(dynamic.getNotifyUrl()).thenReturn("https://notify.example.test/yq-admin/yop-callback/paySuccess");
        when(dynamic.getReturnUrl()).thenReturn("https://front.example.test/payment/return");
        when(dynamic.getExpireMinutes()).thenReturn(5);
        var gateway = mock(YeepayGatewayService.class);
        var result = new JSONObject(); result.put("uniqueOrderNo","CHANNEL"); result.put("cashierUrl","https://cashier.example.test/test");
        when(gateway.execute(any())).thenAnswer(invocation -> {
            YeepayApiRequest request = invocation.getArgument(0);
            assertEquals("/rest/v1.0/cashier/unified/order",request.getApiUri());
            assertEquals("700.00",request.getParameters().get("orderAmount"));
            assertEquals("TEST",request.getParameters().get("merchantNo"));
            assertEquals("ZF_TEST",request.getParameters().get("orderId"));
            assertEquals("https://front.example.test/payment/return?orderId=1&paymentOrderNo=ZF_TEST",request.getParameters().get("returnUrl"));
            return YeepayApiResponse.builder().result(result).build();
        });
        var service = new YeepayCashierServiceImpl(dynamic,properties,gateway);
        var req = YeepayUnifiedOrderReq.builder().businessOrderId(1L).localPaymentOrderId(1L).paymentOrderNo("ZF_TEST").goodsName("测试").orderAmount(new BigDecimal("700")).build();
        assertEquals("https://cashier.example.test/test",service.unifiedOrder(req).getCashierUrl());
        result.put("cashierUrl","javascript:alert(1)");
        assertThrows(BusinessException.class,()->service.unifiedOrder(req));
        result.remove("cashierUrl");
        assertThrows(BusinessException.class,()->service.unifiedOrder(req));
    }
}
