package cn.yq.modules.studentportal.domain.vo.req.aichat;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentAiChatCreateSessionReq {

    @Size(max = 80, message = "会话标题不能超过80个字符")
    private String title;
}
