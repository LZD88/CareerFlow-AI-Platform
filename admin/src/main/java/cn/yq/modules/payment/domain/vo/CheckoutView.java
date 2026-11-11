package cn.yq.modules.payment.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** 收银台页面只需要的字段；公共查询不会返回姓名、电话、登录凭证或收银台链接。 */
@Data
public class CheckoutView {
    private Long orderId;
    private String orderNo;
    private String orderStatus;
    private BigDecimal receivableAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String paymentOrderNo;
    private String paymentStatus;
    private BigDecimal paymentAmount;
    private String cashierUrl;
    private Date expiresAt;
    private boolean expired;
    private boolean ready;
    private boolean sandbox;
    private List<String> missingConfig;
    private String message;
}
