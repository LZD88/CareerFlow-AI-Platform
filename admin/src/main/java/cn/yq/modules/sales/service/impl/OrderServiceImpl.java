package cn.yq.modules.sales.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.OrderEntity;
import cn.yq.modules.sales.domain.entity.ProductEntity;
import cn.yq.modules.sales.domain.vo.req.OrderCreateReq;
import cn.yq.modules.sales.domain.vo.req.OrderQueryReq;
import cn.yq.modules.sales.domain.vo.req.OrderUpdateReq;
import cn.yq.modules.sales.enums.OrderStatusEnum;
import cn.yq.modules.sales.mapper.OrderMapper;
import cn.yq.modules.sales.mapper.ProductMapper;
import cn.yq.modules.sales.service.OrderService;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.mapper.ClassMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ORDER_NO_RANDOM_LENGTH = 6;
    private static final int STUDENT_CODE_RANDOM_LENGTH = 6;
    private static final int ORDER_NO_MAX_RETRY = 10;
    private static final String DEFAULT_BUSINESS_CODE = "DD";
    private static final String DEFAULT_STUDENT_CODE_PREFIX = "XS";

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final ClassMapper classMapper;

    @Override
    public PageResult<OrderEntity> page(OrderQueryReq request) {
        Page<OrderEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<OrderEntity> records = orderMapper.selectPage(request.getKeyword(), request.getOrderStatus(), request.getFulfillmentStatus());
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public OrderEntity detail(Long id) {
        return requireOrder(id);
    }

    @Override
    public OrderEntity findLatestByStudentCode(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) {
            return null;
        }
        return orderMapper.selectLatestByStudentCode(studentCode.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrderCreateReq request) {
        ProductEntity product = requireProduct(request.getProductId());
        ClassEntity classEntity = requireClassIfNeeded(product, request.getClassId());
        String orderNo = generateOrderNo(product);
        String studentCode = generateStudentCode();
        OrderEntity entity = buildOrderEntity(
                new OrderEntity(),
                orderNo,
                request.getStudentName(),
                request.getPhone(),
                studentCode,
                product,
                classEntity,
                request.getDiscountAmount()
        );
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        orderMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OrderUpdateReq request) {
        orderMapper.lockById(id);
        OrderEntity entity = requireOrder(id);
        requireNoOpenPayment(entity);
        if (!OrderStatusEnum.UNPAID.getCode().equals(entity.getOrderStatus())) {
            throw new BusinessException("已收款或已关闭的报名订单不能修改，请通过售后流程处理");
        }
        ProductEntity product = requireProduct(request.getProductId());
        ClassEntity classEntity = requireClassIfNeeded(product, request.getClassId());
        BigDecimal currentPaidAmount = entity.getPaidAmount() == null ? BigDecimal.ZERO : entity.getPaidAmount();
        buildOrderEntity(
                entity,
                entity.getOrderNo(),
                request.getStudentName(),
                request.getPhone(),
                entity.getStudentCode(),
                product,
                classEntity,
                request.getDiscountAmount()
        );
        entity.setPaidAmount(currentPaidAmount);
        entity.setOrderStatus(resolveStatus(entity.getReceivableAmount(), currentPaidAmount).getCode());
        entity.setUpdatedAt(new Date());
        orderMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(String orderNo, BigDecimal paidAmount) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new BusinessException("订单编号不能为空");
        }
        orderMapper.lockByOrderNo(orderNo.trim());
        OrderEntity entity = orderMapper.selectByOrderNo(orderNo.trim());
        if (entity == null) {
            throw new BusinessException("订单不存在");
        }
        BigDecimal callbackPaidAmount = paidAmount == null ? entity.getReceivableAmount() : paidAmount;
        BigDecimal currentPaidAmount = entity.getPaidAmount() == null ? BigDecimal.ZERO : entity.getPaidAmount();
        BigDecimal nextPaidAmount = currentPaidAmount.max(callbackPaidAmount);
        entity.setPaidAmount(nextPaidAmount);
        entity.setOrderStatus(resolveStatus(entity.getReceivableAmount(), nextPaidAmount).getCode());
        entity.setUpdatedAt(new Date());
        orderMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        orderMapper.lockById(id);
        OrderEntity entity = requireOrder(id);
        requireNoOpenPayment(entity);
        if (!OrderStatusEnum.UNPAID.getCode().equals(entity.getOrderStatus())) {
            throw new BusinessException("已收款或已关闭的报名订单不能删除");
        }
        orderMapper.deleteById(id);
    }

    private void requireNoOpenPayment(OrderEntity order) {
        if (orderMapper.countOpenPayments(order.getOrderNo()) > 0)
            throw new BusinessException(409, "订单已有待确认支付，核实渠道结果后才能修改或删除");
    }

    private OrderEntity requireOrder(Long id) {
        OrderEntity entity = orderMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("订单不存在");
        }
        return entity;
    }

    private ProductEntity requireProduct(Long productId) {
        ProductEntity entity = productMapper.selectById(productId);
        if (entity == null) {
            throw new BusinessException("产品不存在");
        }
        return entity;
    }

    private ClassEntity requireClassIfNeeded(ProductEntity product, Long classId) {
        boolean offlineProduct = product != null && "OFFLINE".equals(product.getStudyMode());
        if (!offlineProduct) {
            return null;
        }
        if (classId == null) {
            throw new BusinessException("线下产品必须选择班级期数");
        }
        ClassEntity classEntity = classMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException("班级不存在");
        }
        return classEntity;
    }

    private String generateOrderNo(ProductEntity product) {
        String businessCode = resolveBusinessCode(product.getProductCode());
        String datePart = LocalDate.now().format(ORDER_DATE_FORMATTER);
        for (int i = 0; i < ORDER_NO_MAX_RETRY; i++) {
            String orderNo = businessCode + datePart + randomDigits(ORDER_NO_RANDOM_LENGTH);
            if (orderMapper.selectByOrderNo(orderNo) == null) {
                return orderNo;
            }
        }
        throw new BusinessException("订单编号生成失败，请稍后重试");
    }

    private String generateStudentCode() {
        String datePart = LocalDate.now().format(ORDER_DATE_FORMATTER);
        for (int i = 0; i < ORDER_NO_MAX_RETRY; i++) {
            String studentCode = DEFAULT_STUDENT_CODE_PREFIX + datePart + randomDigits(STUDENT_CODE_RANDOM_LENGTH);
            if (orderMapper.selectByStudentCode(studentCode) == null) {
                return studentCode;
            }
        }
        throw new BusinessException("学员编号生成失败，请稍后重试");
    }

    private String resolveBusinessCode(String productCode) {
        if (productCode == null) {
            return DEFAULT_BUSINESS_CODE;
        }
        String normalized = productCode.trim().toUpperCase();
        if (normalized.length() >= 2) {
            return normalized.substring(0, 2);
        }
        if (normalized.length() == 1) {
            return normalized + "0";
        }
        return DEFAULT_BUSINESS_CODE;
    }

    private String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    private OrderEntity buildOrderEntity(OrderEntity entity,
                                         String orderNo,
                                         String studentName,
                                         String phone,
                                         String studentCode,
                                         ProductEntity product,
                                         ClassEntity classEntity,
                                         BigDecimal discountAmount) {
        BigDecimal safeDiscount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        if (safeDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠金额不能小于 0");
        }
        BigDecimal productAmount = product.getProductPrice();
        BigDecimal receivableAmount = productAmount.subtract(safeDiscount);
        if (receivableAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("优惠金额不能大于产品金额");
        }
        entity.setOrderNo(orderNo.trim());
        entity.setStudentName(studentName.trim());
        entity.setPhone(phone.trim());
        entity.setStudentCode(studentCode.trim());
        entity.setProductId(product.getId());
        entity.setClassId(classEntity == null ? null : classEntity.getId());
        entity.setClassTerm(classEntity == null ? null : classEntity.getClassTerm());
        entity.setProductAmount(productAmount);
        entity.setDiscountAmount(safeDiscount);
        entity.setReceivableAmount(receivableAmount);
        if (entity.getPaidAmount() == null) {
            entity.setPaidAmount(BigDecimal.ZERO);
        }
        entity.setOrderStatus(resolveStatus(receivableAmount, entity.getPaidAmount()).getCode());
        return entity;
    }

    private OrderStatusEnum resolveStatus(BigDecimal receivableAmount, BigDecimal paidAmount) {
        BigDecimal safePaidAmount = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        if (safePaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return OrderStatusEnum.UNPAID;
        }
        if (safePaidAmount.compareTo(receivableAmount) >= 0) {
            return OrderStatusEnum.PAID;
        }
        return OrderStatusEnum.PARTIAL_PAID;
    }
}
