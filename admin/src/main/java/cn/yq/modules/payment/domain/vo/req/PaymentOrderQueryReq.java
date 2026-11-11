package cn.yq.modules.payment.domain.vo.req;

import lombok.Data;

@Data
public class PaymentOrderQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String paymentStatus;

    private String paymentChannel;
}
