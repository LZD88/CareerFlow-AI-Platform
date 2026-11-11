package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentHomeworkUploadPresignReq {

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String fileName;

    @Size(max = 100, message = "文件类型长度不能超过100个字符")
    private String contentType;
}
