package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HomeworkSubmissionReviewReq {

    @NotBlank(message = "批改结果不能为空")
    private String status;

    @DecimalMin(value = "0", message = "得分不能小于0")
    @DecimalMax(value = "100", message = "得分不能大于100")
    private BigDecimal score;

    @Size(max = 4000, message = "评语不能超过4000个字符")
    private String teacherComment;
}
