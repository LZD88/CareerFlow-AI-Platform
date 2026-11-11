package cn.yq.modules.payment.service.impl;

import cn.yq.modules.payment.domain.entity.PaymentChannelOrderEntity;
import cn.yq.modules.payment.mapper.PaymentChannelOrderMapper;
import cn.yq.modules.payment.service.PaymentChannelOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class PaymentChannelOrderServiceImpl implements PaymentChannelOrderService {

    private final PaymentChannelOrderMapper paymentChannelOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(PaymentChannelOrderEntity entity) {
        PaymentChannelOrderEntity existed = paymentChannelOrderMapper.selectByUniqueOrderNo(entity.getUniqueOrderNo());
        if (existed == null) {
            entity.setCreatedAt(new Date());
            entity.setUpdatedAt(new Date());
            paymentChannelOrderMapper.insert(entity);
            return;
        }

        entity.setId(existed.getId());
        entity.setCreatedAt(existed.getCreatedAt());
        entity.setUpdatedAt(new Date());
        paymentChannelOrderMapper.updateById(entity);
    }

    @Override
    public PaymentChannelOrderEntity findLatestByPaymentOrderNo(String paymentOrderNo) {
        if (paymentOrderNo == null || paymentOrderNo.trim().isEmpty()) {
            return null;
        }
        return paymentChannelOrderMapper.selectLatestByPaymentOrderNo(paymentOrderNo.trim());
    }
}
