package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

@Data
public class StudentPaymentCashierRes {

    private Long orderId;

    private String orderNo;

    private Long paymentOrderId;

    private String paymentOrderNo;

    private String uniqueOrderNo;

    private String cashierUrl;
}
