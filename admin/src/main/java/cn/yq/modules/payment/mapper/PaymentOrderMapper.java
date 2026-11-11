package cn.yq.modules.payment.mapper;

import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PaymentOrderMapper {

    @org.apache.ibatis.annotations.Select("select * from sale_payment_order where payment_order_no = #{paymentOrderNo} for update")
    @org.apache.ibatis.annotations.ResultMap("PaymentOrderMap")
    PaymentOrderEntity lockByPaymentOrderNo(@Param("paymentOrderNo") String paymentOrderNo);

    @org.apache.ibatis.annotations.Select("select * from sale_payment_order where order_no = #{orderNo} and payment_channel = 'YEEPAY' and payment_status in ('INIT','PROCESSING','UNIFIED_ORDER_CREATED') order by id desc limit 1")
    @org.apache.ibatis.annotations.ResultMap("PaymentOrderMap")
    PaymentOrderEntity selectOpenByOrderNo(@Param("orderNo") String orderNo);

    @org.apache.ibatis.annotations.Update("update sale_payment_order set last_error_msg = #{message}, updated_at = now() where id = #{id} and payment_status = 'PROCESSING'")
    int recordUncertain(@Param("id") Long id, @Param("message") String message);

    int insert(PaymentOrderEntity entity);

    PaymentOrderEntity selectByPaymentOrderNo(@Param("paymentOrderNo") String paymentOrderNo);

    PaymentOrderEntity selectLatestByOrderNo(@Param("orderNo") String orderNo);

    PaymentOrderEntity selectByUniqueOrderNo(@Param("uniqueOrderNo") String uniqueOrderNo);

    List<PaymentOrderEntity> selectPage(@Param("keyword") String keyword,
                                        @Param("paymentStatus") String paymentStatus,
                                        @Param("paymentChannel") String paymentChannel);

    int updateProcessingStatus(@Param("id") Long id,
                               @Param("fromStatus") String fromStatus,
                               @Param("toStatus") String toStatus);

    int updateUnifiedOrderResult(PaymentOrderEntity entity);

    int updateFailure(@Param("id") Long id,
                      @Param("paymentStatus") String paymentStatus,
                      @Param("lastErrorMsg") String lastErrorMsg);

    int updateCallbackResult(PaymentOrderEntity entity);

    int updateStatusByIdAndCurrentStatus(@Param("id") Long id,
                                         @Param("fromStatus") String fromStatus,
                                         @Param("toStatus") String toStatus,
                                         @Param("lastErrorMsg") String lastErrorMsg);
}
