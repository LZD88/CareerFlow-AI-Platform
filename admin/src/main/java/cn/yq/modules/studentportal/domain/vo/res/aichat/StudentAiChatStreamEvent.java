package cn.yq.modules.studentportal.domain.vo.res.aichat;

import lombok.Data;

@Data
public class StudentAiChatStreamEvent {

    private String type;

    private String message;

    private Object data;

    private Long timestamp;

    public static StudentAiChatStreamEvent of(String type, String message, Object data) {
        StudentAiChatStreamEvent event = new StudentAiChatStreamEvent();
        event.setType(type);
        event.setMessage(message);
        event.setData(data);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
}
