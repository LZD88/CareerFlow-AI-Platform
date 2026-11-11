package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentHomeworkSubmitReq {

    @Size(max = 4000, message = "提交说明不能超过4000个字符")
    private String submitContent;

    @Size(max = 255, message = "附件名称不能超过255个字符")
    private String attachmentName;

    @Size(max = 500, message = "附件地址不能超过500个字符")
    private String attachmentUrl;

    @Size(max = 50, message = "附件类型不能超过50个字符")
    private String attachmentType;
}
