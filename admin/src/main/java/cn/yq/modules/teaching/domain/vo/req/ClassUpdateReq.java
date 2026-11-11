package cn.yq.modules.teaching.domain.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class ClassUpdateReq {

    @NotNull(message = "班级期数不能为空")
    @Min(value = 1, message = "班级期数至少为 1")
    private Integer classTerm;

    @NotNull(message = "校区不能为空")
    private Long campusId;

    @NotNull(message = "班主任不能为空")
    private Long headTeacherId;

    @NotBlank(message = "班级状态不能为空")
    private String classStatus;

    @NotNull(message = "开班时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date startTime;

    @NotNull(message = "课程不能为空")
    private Long courseId;

    @NotNull(message = "班级人数不能为空")
    @Min(value = 0, message = "班级人数不能小于 0")
    private Integer studentCount;
}
