package cn.yq.modules.sales.enums;

import cn.yq.common.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatusEnum {

    UNPAID("UNPAID", "未支付"),
    PARTIAL_PAID("PARTIAL_PAID", "部分支付"),
    PAID("PAID", "已支付"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    public static OrderStatusEnum fromCode(String code) {
        for (OrderStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new BusinessException("不支持的订单状态");
    }
}
