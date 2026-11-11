package cn.yq.modules.teaching.domain.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class HomeworkAssignmentSaveReq {

    @NotNull(message = "班级不能为空")
    private Long classId;

    @NotNull(message = "作业日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date homeworkDate;

    @NotNull(message = "课程不能为空")
    private Long courseId;

    private Long courseDetailId;

    @NotBlank(message = "课程内容不能为空")
    private String courseContentSnapshot;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String description;

    private String attachmentName;

    private String attachmentUrl;

    private String attachmentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date deadline;

    @NotNull(message = "是否允许重交不能为空")
    private Boolean allowResubmit;
}
