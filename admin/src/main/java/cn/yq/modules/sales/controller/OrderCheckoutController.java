package cn.yq.modules.sales.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.payment.domain.vo.CheckoutView;
import cn.yq.modules.payment.service.OrderCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderCheckoutController {
    private final OrderCheckoutService checkout;

    @GetMapping("/api/payments/orders/{id}/checkout")
    public ApiResponse<CheckoutView> preview(@PathVariable Long id) { return ApiResponse.success(checkout.preview(id)); }

    @PostMapping("/api/payments/orders/{id}/checkout")
    public ApiResponse<CheckoutView> create(@PathVariable Long id) { return ApiResponse.success(checkout.create(id)); }

    @GetMapping("/student-api/public/orders/{id}/checkout-status")
    public ApiResponse<CheckoutView> status(@PathVariable Long id, @RequestParam String paymentOrderNo) {
        return ApiResponse.success(checkout.publicStatus(id, paymentOrderNo));
    }
}
