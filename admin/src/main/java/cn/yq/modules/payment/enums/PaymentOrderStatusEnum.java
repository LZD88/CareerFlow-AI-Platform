package cn.yq.modules.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentOrderStatusEnum {

    INIT("INIT", "待发起"),
    PROCESSING("PROCESSING", "下单中"),
    UNIFIED_ORDER_CREATED("UNIFIED_ORDER_CREATED", "已创建收银台单"),
    PAY_SUCCESS("PAY_SUCCESS", "支付成功"),
    CLOSED("CLOSED", "已关闭"),
    FAILED("FAILED", "下单失败");

    private final String code;
    private final String label;
}
