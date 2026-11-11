package cn.yq.modules.sales.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.domain.vo.req.OrderCreateReq;
import cn.yq.modules.sales.domain.vo.req.OrderQueryReq;
import cn.yq.modules.sales.domain.vo.req.OrderUpdateReq;
import cn.yq.modules.sales.service.OrderService;
import cn.yq.modules.sales.service.OrderEnrollmentService;
import cn.yq.modules.studentportal.domain.vo.req.StudentEnrollmentSubmitReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderEnrollmentService orderEnrollmentService;

    /** 已支付报名订单办理入班，同时建立学员档案并关联课程商品与班级。 */
    @PostMapping("/{id}/enrollment")
    public ApiResponse<Long> enroll(@PathVariable Long id,
                                   @RequestBody @Valid StudentEnrollmentSubmitReq request) {
        return ApiResponse.success("入班办理成功", orderEnrollmentService.enroll(id, request));
    }

    /**
     * 订单分页查询。
     */
    @GetMapping
    public ApiResponse<PageResult<OrderEntity>> page(OrderQueryReq request) {
        return ApiResponse.success(orderService.page(request));
    }

    /**
     * 查询订单详情，供编辑弹窗回填使用。
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.detail(id));
    }

    /**
     * 创建订单。
     * 报名订单默认按未支付处理，由后端计算课程商品金额、应收金额和初始状态。
     */
    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid OrderCreateReq request) {
        return ApiResponse.success("创建成功", orderService.create(request));
    }

    /**
     * 更新订单基础信息。
     * 订单金额仍以后端计算为准，避免前端传值造成金额不一致。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid OrderUpdateReq request) {
        orderService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 删除订单。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
