package cn.yq.modules.sales.domain.vo.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateReq {

    @NotBlank(message = "学员姓名不能为空")
    private String studentName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "产品不能为空")
    private Long productId;

    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "优惠金额不能小于 0")
    private BigDecimal discountAmount;

    private Long classId;
}
