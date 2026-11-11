package cn.yq.modules.teaching.domain.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class ClassScheduleArrangeReq {

    @NotNull(message = "上课日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date scheduleDate;

    @NotBlank(message = "新增课程类型不能为空")
    private String scheduleType;

    private String stageName;

    private String courseContent;

    private Long teacherId;
}
