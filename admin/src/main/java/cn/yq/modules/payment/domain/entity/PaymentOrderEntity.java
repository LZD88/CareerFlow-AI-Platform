package cn.yq.modules.payment.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentOrderEntity {

    private Long id;

    private String paymentOrderNo;

    private String orderNo;

    private String studentCode;

    private String studentName;

    private String phone;

    private String productName;

    private BigDecimal paymentAmount;

    private String paymentChannel;

    private String paymentStatus;

    private String uniqueOrderNo;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String cashierUrl;

    private Date expiresAt;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String merchantNo;

    private String lastErrorMsg;

    private Date createdAt;

    private Date updatedAt;
}
