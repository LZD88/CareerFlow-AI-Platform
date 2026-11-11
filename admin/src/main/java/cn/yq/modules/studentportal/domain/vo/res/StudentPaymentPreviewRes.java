package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentPaymentPreviewRes {

    private Long orderId;

    private String orderNo;

    private String studentCode;

    private String studentName;

    private String productName;

    private BigDecimal receivableAmount;

    private BigDecimal paidAmount;

    private BigDecimal remainingAmount;

    private String paymentOrderNo;
}
