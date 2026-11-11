package cn.yq.modules.sales.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 产品实体。
 */
@Data
public class ProductEntity {

    private Long id;

    private String productCode;

    private String productName;

    private String studyMode;

    private BigDecimal productPrice;

    private Date createdAt;

    private Date updatedAt;
}
