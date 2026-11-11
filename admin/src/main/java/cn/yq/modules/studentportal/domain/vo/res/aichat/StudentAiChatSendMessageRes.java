package cn.yq.modules.studentportal.domain.vo.res.aichat;

import lombok.Data;

@Data
public class StudentAiChatSendMessageRes {

    private StudentAiChatMessageRes userMessage;

    private StudentAiChatMessageRes assistantMessage;
}
