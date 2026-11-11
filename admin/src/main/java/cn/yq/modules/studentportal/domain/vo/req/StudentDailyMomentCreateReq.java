package cn.yq.modules.studentportal.domain.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
public class StudentDailyMomentCreateReq {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @Size(max = 2000, message = "随想长度不能超过2000个字符")
    private String contentText;

    @Size(max = 3, message = "最多上传3个素材文件")
    private List<String> fileUrls;

    @NotNull(message = "素材时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date recordTime;
}
