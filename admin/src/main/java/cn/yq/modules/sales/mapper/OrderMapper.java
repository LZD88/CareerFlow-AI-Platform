package cn.yq.modules.sales.mapper;

import cn.yq.modules.sales.domain.entity.OrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderMapper {

    @org.apache.ibatis.annotations.Select("select count(*) from sale_payment_order where order_no = #{orderNo} and payment_channel = 'YEEPAY' and payment_status in ('INIT','PROCESSING','UNIFIED_ORDER_CREATED')")
    int countOpenPayments(@Param("orderNo") String orderNo);

    int insert(OrderEntity entity);

    int updateById(OrderEntity entity);

    OrderEntity selectById(@Param("id") Long id);

    Long lockById(@Param("id") Long id);

    Long lockByOrderNo(@Param("orderNo") String orderNo);

    OrderEntity selectByOrderNo(@Param("orderNo") String orderNo);

    OrderEntity selectByStudentCode(@Param("studentCode") String studentCode);

    OrderEntity selectLatestByStudentCode(@Param("studentCode") String studentCode);

    List<OrderEntity> selectPage(@Param("keyword") String keyword,
                                @Param("orderStatus") String orderStatus,
                                @Param("fulfillmentStatus") String fulfillmentStatus);

    int deleteById(@Param("id") Long id);
}
