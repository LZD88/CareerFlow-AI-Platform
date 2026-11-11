package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseUpdateReq {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @NotNull(message = "课程天数不能为空")
    @Min(value = 1, message = "课程天数至少为 1 天")
    private Integer courseDays;

    @NotBlank(message = "飞书资料路径不能为空")
    private String feishuMaterialPath;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
