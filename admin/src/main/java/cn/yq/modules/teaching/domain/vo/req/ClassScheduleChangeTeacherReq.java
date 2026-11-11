package cn.yq.modules.teaching.domain.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class ClassScheduleChangeTeacherReq {

    @NotNull(message = "上课日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date scheduleDate;

    @NotNull(message = "授课老师不能为空")
    private Long teacherId;
}
