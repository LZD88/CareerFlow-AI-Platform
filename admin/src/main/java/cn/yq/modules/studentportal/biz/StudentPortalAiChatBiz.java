package cn.yq.modules.studentportal.biz;

import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentAiChatCreateSessionReq;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentAiChatSendMessageReq;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatMessageRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatSendMessageRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatSessionRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatStreamEvent;

import java.util.List;
import java.util.function.Consumer;

public interface StudentPortalAiChatBiz {

    StudentAiChatSessionRes createSession(StudentAiChatCreateSessionReq request);

    List<StudentAiChatSessionRes> listSessions();

    List<StudentAiChatMessageRes> listMessages(Long sessionId);

    StudentAiChatSendMessageRes sendMessage(Long sessionId, StudentAiChatSendMessageReq request);

    void streamMessage(Long sessionId, StudentAiChatSendMessageReq request, Consumer<StudentAiChatStreamEvent> eventConsumer);

    void deleteSession(Long sessionId);
}
