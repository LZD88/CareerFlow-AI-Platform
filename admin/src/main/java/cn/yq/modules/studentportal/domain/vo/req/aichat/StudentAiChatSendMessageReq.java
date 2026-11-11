package cn.yq.modules.studentportal.domain.vo.req.aichat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentAiChatSendMessageReq {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "单次提问不能超过2000个字符")
    private String content;
}
