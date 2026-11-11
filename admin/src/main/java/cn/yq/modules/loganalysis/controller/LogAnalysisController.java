package cn.yq.modules.loganalysis.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.loganalysis.domain.vo.req.LogDiagnoseReq;
import cn.yq.modules.loganalysis.domain.vo.req.LogSearchReq;
import cn.yq.modules.loganalysis.domain.vo.res.LogDiagnosisRes;
import cn.yq.modules.loganalysis.domain.vo.res.LogDiagnosisStreamEvent;
import cn.yq.modules.loganalysis.domain.vo.res.LogSearchRes;
import cn.yq.modules.loganalysis.service.LogAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 日志分析接口入口。
 *
 * <p>前端“日志分析”页面主要调用这里：</p>
 * <p>1. /search：按查询语句和时间范围直接看火山日志。</p>
 * <p>2. /diagnose/stream：按 guid 执行诊断，并用 SSE 返回进度和最终结果。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/log-analysis")
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    /**
     * 直接查询火山日志。
     *
     * <p>这个接口用于验证日志查询链路，也方便研发手动检索原始日志。</p>
     */
    @PostMapping("/search")
    public ApiResponse<LogSearchRes> search(@RequestBody @Valid LogSearchReq request) {
        return ApiResponse.success(logAnalysisService.searchLogs(request));
    }

    /**
     * 普通阻塞式诊断接口。
     *
     * <p>现在前端优先使用流式接口，这个接口保留给调试或非流式调用。</p>
     */
    @PostMapping("/diagnose")
    public ApiResponse<LogDiagnosisRes> diagnose(@RequestBody @Valid LogDiagnoseReq request) {
        return ApiResponse.success(logAnalysisService.diagnoseByGuid(request));
    }

    /**
     * 流式诊断接口。
     *
     * <p>诊断可能涉及日志查询、结构化提取、Python Agent 调用和模型分析。
     * 用 SSE 可以把每一步进度推给前端，避免页面长时间无反馈。</p>
     */
    @PostMapping(value = "/diagnose/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnoseStream(@RequestBody @Valid LogDiagnoseReq request) {
        SseEmitter emitter = new SseEmitter(75_000L);
        CompletableFuture.runAsync(() -> {
            try {
                logAnalysisService.diagnoseByGuid(request, event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (Exception ex) {
                sendEvent(emitter, buildErrorEvent(ex));
                emitter.complete();
            }
        });
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, LogDiagnosisStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name("message").data(event));
        } catch (IOException ex) {
            throw new IllegalStateException("发送 SSE 事件失败", ex);
        }
    }

    private LogDiagnosisStreamEvent buildErrorEvent(Exception ex) {
        LogDiagnosisStreamEvent event = new LogDiagnosisStreamEvent();
        event.setStage("ERROR");
        event.setStatus("ERROR");
        event.setMessage(ex.getMessage());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exception", ex.getClass().getSimpleName());
        event.setData(data);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
}
