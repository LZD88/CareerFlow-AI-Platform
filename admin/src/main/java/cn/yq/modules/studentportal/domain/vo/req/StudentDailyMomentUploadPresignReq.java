package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentDailyMomentUploadPresignReq {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    private String contentType;
}
