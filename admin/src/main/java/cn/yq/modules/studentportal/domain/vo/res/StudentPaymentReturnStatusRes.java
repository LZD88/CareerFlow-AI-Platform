package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StudentPaymentReturnStatusRes extends StudentPortalLoginRes {

    private Long orderId;

    private String orderNo;

    private String orderStatus;

    private String paymentOrderNo;

    private String paymentStatus;

    private String message;
}
