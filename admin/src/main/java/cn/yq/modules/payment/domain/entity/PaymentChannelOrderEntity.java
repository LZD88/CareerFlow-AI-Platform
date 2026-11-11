package cn.yq.modules.payment.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentChannelOrderEntity {

    private Long id;

    private String paymentOrderNo;

    private String uniqueOrderNo;

    private String merchantNo;

    private String orderId;

    private String channelOrderId;

    private BigDecimal orderAmount;

    private BigDecimal payAmount;

    private String status;

    private String paySuccessDate;

    private String channel;

    private String payerInfo;

    private String failCode;

    private String failReason;

    private String channelTrxId;

    private String bankOrderId;

    private String callbackType;

    private String callbackBody;

    private Date callbackTime;

    private Date createdAt;

    private Date updatedAt;
}
