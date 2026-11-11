package cn.yq.modules.studentportal.domain.vo.res.aichat;

import lombok.Data;

import java.util.Map;

@Data
public class PythonStudentChatStreamEvent {

    /**
     * Python 返回的事件类型。
     * 目前支持 START / TOKEN / DONE / ERROR，后续 RAG 阶段会扩展 SOURCE。
     */
    private String type;

    private String message;

    private String delta;

    private String finishReason;

    private Map<String, Object> usage;
}
