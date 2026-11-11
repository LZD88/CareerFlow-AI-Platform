package cn.yq.modules.sales.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体。
 */
@Data
public class OrderEntity {

    private Long id;

    private String orderNo;

    private String studentName;

    private String phone;

    private Long productId;

    private String productName;

    private String productStudyMode;

    private Long classId;

    private Integer classTerm;

    private String studentCode;

    private BigDecimal productAmount;

    private BigDecimal discountAmount;

    private BigDecimal receivableAmount;

    private BigDecimal paidAmount;

    private String orderStatus;

    /** 关联成功的学员档案ID。 */
    private Long studentId;

    /** PENDING_PAYMENT / PENDING_ENROLLMENT / ENROLLED / CLOSED。 */
    private String fulfillmentStatus;

    private Date createdAt;

    private Date updatedAt;
}
