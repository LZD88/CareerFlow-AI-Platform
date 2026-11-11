package cn.yq.modules.sales.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.payment.domain.entity.PaymentChannelOrderEntity;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.domain.vo.req.PaymentOrderQueryReq;
import cn.yq.modules.payment.service.PaymentChannelOrderService;
import cn.yq.modules.payment.service.PaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;

    private final PaymentChannelOrderService paymentChannelOrderService;

    /**
     * 支付订单分页查询。
     */
    @GetMapping
    public ApiResponse<PageResult<PaymentOrderEntity>> page(PaymentOrderQueryReq request) {
        return ApiResponse.success(paymentOrderService.page(request));
    }

    /**
     * 查询支付订单对应的最新渠道扩展信息。
     */
    @GetMapping("/{paymentOrderNo}/channel-order")
    public ApiResponse<PaymentChannelOrderEntity> channelOrder(@PathVariable String paymentOrderNo) {
        return ApiResponse.success(paymentChannelOrderService.findLatestByPaymentOrderNo(paymentOrderNo));
    }
}
