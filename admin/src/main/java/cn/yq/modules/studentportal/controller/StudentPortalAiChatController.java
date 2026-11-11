package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentportal.biz.StudentPortalAiChatBiz;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentAiChatCreateSessionReq;
import cn.yq.modules.studentportal.domain.vo.req.aichat.StudentAiChatSendMessageReq;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatMessageRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatSendMessageRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatSessionRes;
import cn.yq.modules.studentportal.domain.vo.res.aichat.StudentAiChatStreamEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api/ai-chat")
public class StudentPortalAiChatController {

    private final StudentPortalAiChatBiz studentPortalAiChatBiz;

    @PostMapping("/sessions")
    public ApiResponse<StudentAiChatSessionRes> createSession(@RequestBody(required = false) @Valid StudentAiChatCreateSessionReq request) {
        return ApiResponse.success("会话创建成功", studentPortalAiChatBiz.createSession(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<StudentAiChatSessionRes>> listSessions() {
        return ApiResponse.success(studentPortalAiChatBiz.listSessions());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<StudentAiChatMessageRes>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.success(studentPortalAiChatBiz.listMessages(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<StudentAiChatSendMessageRes> sendMessage(@PathVariable Long sessionId,
                                                                @RequestBody @Valid StudentAiChatSendMessageReq request) {
        return ApiResponse.success(studentPortalAiChatBiz.sendMessage(sessionId, request));
    }

    @PostMapping(value = "/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable Long sessionId,
                                    @RequestBody @Valid StudentAiChatSendMessageReq request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        CompletableFuture.runAsync(() -> {
            try {
                StudentPortalAccessContextHolder.set(accessContext);
                studentPortalAiChatBiz.streamMessage(sessionId, request, event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (Exception ex) {
                sendEvent(emitter, StudentAiChatStreamEvent.of("ERROR", ex.getMessage(), null));
                emitter.complete();
            } finally {
                StudentPortalAccessContextHolder.clear();
            }
        });
        return emitter;
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        studentPortalAiChatBiz.deleteSession(sessionId);
        return ApiResponse.success("删除成功", null);
    }

    private void sendEvent(SseEmitter emitter, StudentAiChatStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name("message").data(event));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 SSE 事件失败", ex);
        }
    }
}
