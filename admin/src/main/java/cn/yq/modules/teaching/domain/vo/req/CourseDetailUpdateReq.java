package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseDetailUpdateReq {

    @NotBlank(message = "阶段不能为空")
    private String stageName;

    @NotNull(message = "第几天课不能为空")
    @Min(value = 1, message = "第几天课至少为 1")
    private Integer dayNum;

    @NotBlank(message = "课程内容不能为空")
    private String courseContent;
}
