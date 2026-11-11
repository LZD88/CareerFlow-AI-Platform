package cn.yq.modules.sales.service;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.domain.vo.req.OrderUpdateReq;
import cn.yq.modules.sales.mapper.*;
import cn.yq.modules.sales.service.impl.OrderServiceImpl;
import cn.yq.modules.teaching.mapper.ClassMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleTest {
    @Mock OrderMapper orders;
    @Mock ProductMapper products;
    @Mock ClassMapper classes;
    @InjectMocks OrderServiceImpl service;

    @Test void paidOrderCannotBeEdited() {
        var order = new OrderEntity();
        order.setOrderStatus("PAID");
        when(orders.selectById(1L)).thenReturn(order);
        assertThrows(BusinessException.class, () -> service.update(1L, new OrderUpdateReq()));
        verify(orders).lockById(1L);
        verify(orders, never()).updateById(any());
        verifyNoInteractions(products, classes);
    }
    @Test void partiallyPaidOrderCannotBeDeleted() {
        var order = new OrderEntity();
        order.setOrderStatus("PARTIAL_PAID");
        when(orders.selectById(1L)).thenReturn(order);
        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(orders, never()).deleteById(any());
    }
}
