package cn.yq.modules.sales.domain.vo.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateReq {

    @NotBlank(message = "产品编号不能为空")
    private String productCode;

    @NotBlank(message = "产品名称不能为空")
    private String productName;

    @NotBlank(message = "学习方式不能为空")
    private String studyMode;

    @NotNull(message = "产品价格不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "产品价格不能小于 0")
    private BigDecimal productPrice;
}
