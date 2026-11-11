package cn.yq.modules.payment.mapper;

import cn.yq.modules.payment.domain.entity.PaymentChannelOrderEntity;
import org.apache.ibatis.annotations.Param;

public interface PaymentChannelOrderMapper {

    int insert(PaymentChannelOrderEntity entity);

    int updateById(PaymentChannelOrderEntity entity);

    PaymentChannelOrderEntity selectByUniqueOrderNo(@Param("uniqueOrderNo") String uniqueOrderNo);

    PaymentChannelOrderEntity selectLatestByPaymentOrderNo(@Param("paymentOrderNo") String paymentOrderNo);
}
