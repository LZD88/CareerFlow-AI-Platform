package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentPaymentUnifiedOrderReq {

    @NotBlank(message = "支付订单号不能为空")
    private String paymentOrderNo;
}
