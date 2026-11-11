package cn.yq.modules.sales.service;

import java.math.BigDecimal;

import cn.yq.common.api.PageResult;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.domain.vo.req.OrderCreateReq;
import cn.yq.modules.sales.domain.vo.req.OrderQueryReq;
import cn.yq.modules.sales.domain.vo.req.OrderUpdateReq;

public interface OrderService {

    PageResult<OrderEntity> page(OrderQueryReq request);

    OrderEntity detail(Long id);

    OrderEntity findLatestByStudentCode(String studentCode);

    Long create(OrderCreateReq request);

    void update(Long id, OrderUpdateReq request);

    void markPaid(String orderNo, BigDecimal paidAmount);

    void delete(Long id);
}
