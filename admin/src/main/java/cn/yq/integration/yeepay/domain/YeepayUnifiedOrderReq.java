package cn.yq.integration.yeepay.domain;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class YeepayUnifiedOrderReq {

    java.util.Date expiresAt;

    Long localPaymentOrderId;

    Long businessOrderId;

    String paymentOrderNo;

    String businessOrderNo;

    BigDecimal orderAmount;

    String goodsName;

    String studentCode;

    String studentName;

    String phone;
}
