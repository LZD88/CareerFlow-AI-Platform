package cn.yq.modules.payment.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.domain.vo.req.PaymentOrderQueryReq;
import cn.yq.modules.payment.enums.PaymentOrderStatusEnum;
import cn.yq.modules.payment.mapper.PaymentOrderMapper;
import cn.yq.modules.payment.service.PaymentOrderService;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int RANDOM_LENGTH = 8;
    private static final String PREFIX = "ZF";

    private final PaymentOrderMapper paymentOrderMapper;

    @Override
    public PageResult<PaymentOrderEntity> page(PaymentOrderQueryReq request) {
        Page<PaymentOrderEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<PaymentOrderEntity> records = paymentOrderMapper.selectPage(
                trimToNull(request.getKeyword()),
                trimToNull(request.getPaymentStatus()),
                trimToNull(request.getPaymentChannel())
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public String generatePaymentOrderNo() {
        return PREFIX + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderEntity createIfAbsent(OrderEntity order, String paymentOrderNo) {
        PaymentOrderEntity existed = paymentOrderMapper.selectByPaymentOrderNo(paymentOrderNo);
        if (existed != null) {
            validateOwnership(existed, order);
            return existed;
        }

        PaymentOrderEntity entity = new PaymentOrderEntity();
        entity.setPaymentOrderNo(paymentOrderNo.trim());
        entity.setOrderNo(order.getOrderNo());
        entity.setStudentCode(order.getStudentCode());
        entity.setStudentName(order.getStudentName());
        entity.setPhone(order.getPhone());
        entity.setProductName(order.getProductName());
        entity.setPaymentAmount(order.getReceivableAmount().subtract(order.getPaidAmount() == null ? java.math.BigDecimal.ZERO : order.getPaidAmount()));
        if (entity.getPaymentAmount().signum() <= 0) throw new BusinessException(409, "订单已无需收款");
        entity.setPaymentChannel("YEEPAY");
        entity.setPaymentStatus(PaymentOrderStatusEnum.INIT.getCode());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        try {
            paymentOrderMapper.insert(entity);
            return entity;
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(409, "请不要重复下单");
        }
    }

    @Override
    public PaymentOrderEntity findByPaymentOrderNo(String paymentOrderNo) {
        if (paymentOrderNo == null || paymentOrderNo.trim().isEmpty()) {
            return null;
        }
        return paymentOrderMapper.selectByPaymentOrderNo(paymentOrderNo.trim());
    }

    @Override
    public PaymentOrderEntity findLatestByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return null;
        }
        return paymentOrderMapper.selectLatestByOrderNo(orderNo.trim());
    }

    @Override
    public PaymentOrderEntity findByUniqueOrderNo(String uniqueOrderNo) {
        if (uniqueOrderNo == null || uniqueOrderNo.trim().isEmpty()) {
            return null;
        }
        return paymentOrderMapper.selectByUniqueOrderNo(uniqueOrderNo.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markProcessing(Long id) {
        int updated = paymentOrderMapper.updateProcessingStatus(
                id,
                PaymentOrderStatusEnum.INIT.getCode(),
                PaymentOrderStatusEnum.PROCESSING.getCode()
        );
        if (updated > 0) {
            return true;
        }
        return paymentOrderMapper.updateProcessingStatus(
                id,
                PaymentOrderStatusEnum.FAILED.getCode(),
                PaymentOrderStatusEnum.PROCESSING.getCode()
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUnifiedOrderResult(PaymentOrderEntity entity) {
        entity.setPaymentStatus(PaymentOrderStatusEnum.UNIFIED_ORDER_CREATED.getCode());
        entity.setUpdatedAt(new Date());
        paymentOrderMapper.updateUnifiedOrderResult(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long id, String lastErrorMsg) {
        paymentOrderMapper.updateFailure(id, PaymentOrderStatusEnum.FAILED.getCode(), truncate(lastErrorMsg));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallbackResult(PaymentOrderEntity entity) {
        entity.setUpdatedAt(new Date());
        paymentOrderMapper.updateCallbackResult(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeIfUnifiedOrderCreated(Long id, String lastErrorMsg) {
        return paymentOrderMapper.updateStatusByIdAndCurrentStatus(
                id,
                PaymentOrderStatusEnum.UNIFIED_ORDER_CREATED.getCode(),
                PaymentOrderStatusEnum.CLOSED.getCode(),
                truncate(lastErrorMsg)
        ) > 0;
    }

    private void validateOwnership(PaymentOrderEntity paymentOrder, OrderEntity order) {
        if (!paymentOrder.getOrderNo().equals(order.getOrderNo())) {
            throw new BusinessException(400, "支付订单号与业务订单不匹配");
        }
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
