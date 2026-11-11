package cn.yq.modules.loganalysis.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.PythonAgentProperties;
import cn.yq.config.VolcengineTlsDynamicProperties;
import cn.yq.config.VolcengineTlsProperties;
import cn.yq.modules.loganalysis.domain.vo.req.LogDiagnoseReq;
import cn.yq.modules.loganalysis.domain.vo.req.LogSearchReq;
import cn.yq.modules.loganalysis.domain.vo.req.PythonDiagnoseReq;
import cn.yq.modules.loganalysis.domain.vo.res.LogDiagnosisRes;
import cn.yq.modules.loganalysis.domain.vo.res.LogDiagnosisStreamEvent;
import cn.yq.modules.loganalysis.domain.vo.res.LogRecordRes;
import cn.yq.modules.loganalysis.domain.vo.res.PythonDiagnoseRes;
import cn.yq.modules.loganalysis.domain.vo.res.LogSearchRes;
import cn.yq.modules.loganalysis.service.LogAnalysisService;
import com.volcengine.model.tls.ClientBuilder;
import com.volcengine.model.tls.ClientConfig;
import com.volcengine.model.tls.request.SearchLogsRequest;
import com.volcengine.model.tls.response.SearchLogsResponseV2;
import com.volcengine.service.tls.TLSLogClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Date;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志分析主实现。
 *
 * <p>按 guid 诊断的完整链路在这里串起来：</p>
 * <p>1. Java 调火山 TLS 查询原始日志。</p>
 * <p>2. Java 从日志中提取请求路径、controller、异常、首个堆栈。</p>
 * <p>3. Java 把结构化上下文交给 Python Agent。</p>
 * <p>4. Python Agent 基于 RAG + LLM 返回根因、证据、置信度和建议。</p>
 * <p>5. Java 合并结果并返回给前端。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisServiceImpl implements LogAnalysisService {

    /**
     * 按 guid 诊断默认回溯 24 小时日志。
     *
     * <p>这个值越大越容易查全，但也会增加 TLS 查询范围和响应时间。</p>
     */
    private static final long DIAGNOSE_LOOKBACK_SECONDS = 24 * 60 * 60L;

    /**
     * 以下正则匹配的是本项目统一日志格式。
     *
     * <p>这些字段主要来自 ControllerLogAspect、GlobalExceptionHandler 和 Java 堆栈输出。</p>
     */
    private static final Pattern REQUEST_URI_PATTERN = Pattern.compile("uri=([^,\\s]+)");
    private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("httpMethod=([^,\\s]+)");
    private static final Pattern REQUEST_ARGS_PATTERN = Pattern.compile("args=(.*)$");
    private static final Pattern CONTROLLER_PATTERN = Pattern.compile("controller=([^,]+)");
    private static final Pattern BUSINESS_EXCEPTION_PATTERN = Pattern.compile("业务异常: code=\\d+, message=([^\\n]+)");
    private static final Pattern EXCEPTION_CLASS_PATTERN = Pattern.compile("([a-zA-Z0-9_$.]+Exception):\\s*([^\\n]+)");
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile("\\bat\\s+([^\\n]+)");

    private final VolcengineTlsProperties tlsProperties;
    private final VolcengineTlsDynamicProperties tlsDynamicProperties;
    private final PythonAgentProperties pythonAgentProperties;

    /**
     * 直接查询火山 TLS 日志。
     *
     * <p>这个方法既服务前端手动查询，也被按 guid 诊断流程复用。
     * request 中 topicId 为空时，会从系统动态配置读取默认 topicId。</p>
     */
    @Override
    public LogSearchRes searchLogs(LogSearchReq request) {
        // 先校验火山 TLS 的基础配置，避免走到 SDK 调用时才报一个难理解的连接错误。
        validateConfig();

        // topicId 支持前端显式传入；如果不传，就使用系统配置里的默认日志 topic。
        String topicId = resolveTopicId(request.getTopicId());

        // 火山 TLS SDK 使用 Unix 秒，前端传入的是 yyyy-MM-dd HH:mm:ss。
        long startTime = toEpochSeconds(request.getStartTime());
        long endTime = toEpochSeconds(request.getEndTime());
        if (endTime < startTime) {
            throw new BusinessException(400, "结束时间不能早于开始时间");
        }

        // query 为空时使用 *，表示按时间范围直接拉取日志。
        SearchLogsRequest searchRequest = new SearchLogsRequest();
        searchRequest.setTopicId(topicId);
        searchRequest.setQuery(StringUtils.hasText(request.getQuery()) ? request.getQuery().trim() : "*");
        searchRequest.setStartTime(startTime);
        searchRequest.setEndTime(endTime);
        searchRequest.setLimit(request.getLimit());

        log.debug("火山日志查询开始: {}", buildRequestLogJson(searchRequest));

        TLSLogClient client = buildClient();
        try {
            // 真正调用火山 TLS。返回值仍是 SDK 原始结构，后面会转换成前端友好的 LogSearchRes。
            SearchLogsResponseV2 response = client.searchLogsV2(searchRequest);
            log.debug("火山日志查询结束: {}", buildResponseLogJson(searchRequest, response));
            return buildResponse(searchRequest, response);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("火山日志查询失败: " + buildRequestLogJson(searchRequest), ex);
            throw new BusinessException(500, "查询火山日志失败: " + ex.getMessage());
        } finally {
            try {
                // TLSLogClient 持有网络连接资源，用完要关闭。
                client.close();
            } catch (Exception ex) {
                log.warn("关闭火山日志客户端失败", ex);
            }
        }
    }

    /**
     * 创建火山 TLS 客户端。
     *
     * <p>每次查询后都会在 finally 中关闭，避免连接资源泄漏。</p>
     */
    private TLSLogClient buildClient() {
        ClientConfig clientConfig = new ClientConfig(
                tlsProperties.getEndpoint(),
                tlsProperties.getRegion(),
                tlsProperties.getAccessKeyId(),
                tlsProperties.getAccessKeySecret()
        );
        try {
            TLSLogClient client = ClientBuilder.newClient(clientConfig);
            client.setTimeout(tlsProperties.getConnectTimeoutMs(), tlsProperties.getReadTimeoutMs());
            return client;
        } catch (Exception ex) {
            throw new BusinessException(500, "初始化火山日志客户端失败: " + ex.getMessage());
        }
    }

    /**
     * 构造查询请求日志。
     *
     * <p>只记录 endpoint、region、topicId、query 等排查必要字段，不记录 AK/SK。</p>
     */
    private String buildRequestLogJson(SearchLogsRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("endpoint", tlsProperties.getEndpoint());
        payload.put("region", tlsProperties.getRegion());
        payload.put("topicId", request.getTopicId());
        payload.put("query", request.getQuery());
        payload.put("startTime", request.getStartTime());
        payload.put("endTime", request.getEndTime());
        payload.put("limit", request.getLimit());
        return JSON.toJSONString(payload);
    }

    @Override
    public LogDiagnosisRes diagnoseByGuid(LogDiagnoseReq request) {
        return diagnoseByGuid(request, null);
    }

    /**
     * 按 guid 执行完整诊断链路。
     *
     * <p>progressConsumer 为空时表示普通阻塞式调用；不为空时会在每个阶段发送
     * SSE 进度事件给前端。</p>
     */
    @Override
    public LogDiagnosisRes diagnoseByGuid(LogDiagnoseReq request, Consumer<LogDiagnosisStreamEvent> progressConsumer) {
        emit(progressConsumer, "START", "STARTED", "开始诊断", null);
        String guid = request.getGuid().trim();

        // 诊断入口只要求用户提供 guid，时间范围由后端固定回溯。
        // 这样前端操作更简单，也避免用户时间范围选错导致查不到日志。
        long endTime = System.currentTimeMillis() / 1000;
        long startTime = endTime - DIAGNOSE_LOOKBACK_SECONDS;

        // 第一步：用 guid 去火山日志检索同一请求链路的所有相关日志。
        LogSearchReq searchReq = new LogSearchReq();
        searchReq.setQuery(guid);
        searchReq.setStartTime(new Date(startTime * 1000));
        searchReq.setEndTime(new Date(endTime * 1000));

        emit(progressConsumer, "QUERY_LOGS", "RUNNING", "正在查询日志", null);
        LogSearchRes searchRes = searchLogs(searchReq);
        Map<String, Object> queryData = new LinkedHashMap<>();
        queryData.put("matchedLogCount", searchRes.getLogs().size());
        emit(progressConsumer, "QUERY_LOGS", "SUCCESS", "日志查询完成", queryData);

        // 第二步：Java 先从日志里提取确定性字段。
        // 这些字段后面会作为 Python Agent 的结构化上下文。
        emit(progressConsumer, "STRUCTURE_LOGS", "RUNNING", "正在提取异常信息和堆栈", null);
        LogDiagnosisRes diagnosis = buildDiagnosis(guid, startTime, endTime, searchRes);
        Map<String, Object> structureData = new LinkedHashMap<>();
        structureData.put("requestUri", diagnosis.getRequestUri());
        structureData.put("httpMethod", diagnosis.getHttpMethod());
        structureData.put("controller", diagnosis.getController());
        structureData.put("exceptionClass", diagnosis.getExceptionClass());
        structureData.put("exceptionMessage", diagnosis.getExceptionMessage());
        structureData.put("topStackFrame", diagnosis.getTopStackFrame());
        emit(progressConsumer, "STRUCTURE_LOGS", "SUCCESS", "结构化提取完成", structureData);

        // 第三步：把 Java 结构化结果交给 Python Agent 做 RAG + LLM 分析。
        // Python 失败不会中断流程，diagnosis 会保留 Java 模拟诊断结果。
        emit(progressConsumer, "CALL_PYTHON", "RUNNING", "正在调用 Python 分析服务", null);
        enrichDiagnosisByPython(diagnosis);
        emit(progressConsumer, "CALL_PYTHON", "SUCCESS", "Python 分析完成", Map.of("analysisSource", diagnosis.getAnalysisSource()));

        // 最后把完整诊断结果作为 DONE 事件发给前端。
        emit(progressConsumer, "DONE", "DONE", "诊断完成", diagnosis);
        return diagnosis;
    }

    /**
     * 构造查询响应摘要日志。
     *
     * <p>这里不打印完整日志正文，只打印状态、命中数等元信息，避免污染应用日志。</p>
     */
    private String buildResponseLogJson(SearchLogsRequest request, SearchLogsResponseV2 response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("endpoint", tlsProperties.getEndpoint());
        payload.put("region", tlsProperties.getRegion());
        payload.put("topicId", request.getTopicId());
        payload.put("query", request.getQuery());
        payload.put("resultStatus", response.getResultStatus());
        payload.put("hitCount", response.getHitCount());
        payload.put("count", response.getCount());
        payload.put("limit", response.getLimit());
        payload.put("isAnalysis", response.isAnalysis());
        payload.put("listOver", response.isListOver());
        payload.put("context", response.getContext());
        return JSON.toJSONString(payload);
    }

    /**
     * 把火山 TLS SDK 响应转换成前端和诊断流程使用的统一结构。
     */
    private LogSearchRes buildResponse(SearchLogsRequest request, SearchLogsResponseV2 response) {
        // rawResponse 保留 SDK 原始 JSON，方便前期联调时核对火山 TLS 返回字段。
        JSONObject responseJson = (JSONObject) JSON.toJSON(response);
        LogSearchRes result = new LogSearchRes();
        result.setTopicId(request.getTopicId());
        result.setQuery(request.getQuery());
        result.setStartTime(request.getStartTime());
        result.setEndTime(request.getEndTime());
        result.setLimit(request.getLimit());
        result.setRawResponse(JSON.toJSONString(responseJson));
        result.setResultStatus(response.getResultStatus());
        result.setIsAnalysis(response.isAnalysis());
        result.setCount((long) response.getCount());
        result.setHitCount((long) response.getHitCount());

        // 前端和诊断流程只关心日志时间与正文，所以这里统一压成 LogRecordRes。
        result.setLogs(parseLogs(response.getLogs()));
        return result;
    }

    /**
     * 从 guid 命中的原始日志中提取结构化诊断上下文。
     *
     * <p>Java 侧先做一层确定性的字段提取，Python Agent 就不需要从零开始理解整段日志。
     * 这一步也是 Python 不可用时的兜底诊断基础。</p>
     */
    private LogDiagnosisRes buildDiagnosis(String guid, long startTime, long endTime, LogSearchRes searchRes) {
        // 先把查询元信息、命中数、原始日志放进诊断结果。
        // 后续字段提取和 Python 分析都会在这个对象上继续补充。
        LogDiagnosisRes diagnosis = new LogDiagnosisRes();
        diagnosis.setGuid(guid);
        diagnosis.setQuery(searchRes.getQuery());
        diagnosis.setStartTime(startTime);
        diagnosis.setEndTime(endTime);
        diagnosis.setMatchedLogCount(searchRes.getLogs().size());
        diagnosis.setRawLogs(searchRes.getLogs().stream()
                .map(LogRecordRes::getContent)
                .filter(Objects::nonNull)
                .toList());

        // 遍历同一个 guid 下的所有日志，尽量从不同日志行里拼出完整异常上下文。
        for (String line : diagnosis.getRawLogs()) {
            // ControllerLogAspect 输出的接口开始/结束日志里通常包含 uri、httpMethod、controller。
            fillIfAbsent(diagnosis::getRequestUri, diagnosis::setRequestUri, extractFirst(line, REQUEST_URI_PATTERN));
            fillIfAbsent(diagnosis::getHttpMethod, diagnosis::setHttpMethod, extractFirst(line, HTTP_METHOD_PATTERN));
            fillIfAbsent(diagnosis::getController, diagnosis::setController, extractFirst(line, CONTROLLER_PATTERN));
            fillIfAbsent(diagnosis::getRequestArgs, diagnosis::setRequestArgs, extractFirst(line, REQUEST_ARGS_PATTERN));

            // GlobalExceptionHandler 对 BusinessException 有固定格式，优先把它识别成业务异常。
            String businessMessage = extractFirst(line, BUSINESS_EXCEPTION_PATTERN);
            if (StringUtils.hasText(businessMessage)) {
                diagnosis.setExceptionClass(firstNonBlank(diagnosis.getExceptionClass(), "BusinessException"));
                fillIfAbsent(diagnosis::getExceptionMessage, diagnosis::setExceptionMessage, businessMessage);
            }

            // 兼容普通 Java 异常格式，例如 java.lang.NullPointerException: xxx。
            Matcher exceptionMatcher = EXCEPTION_CLASS_PATTERN.matcher(line);
            if (exceptionMatcher.find()) {
                fillIfAbsent(diagnosis::getExceptionClass, diagnosis::setExceptionClass, exceptionMatcher.group(1));
                fillIfAbsent(diagnosis::getExceptionMessage, diagnosis::setExceptionMessage, exceptionMatcher.group(2));
            }

            // 首个 at xxx(...) 堆栈通常最接近出错位置，作为后续 RAG 和人工排查入口。
            fillIfAbsent(diagnosis::getTopStackFrame, diagnosis::setTopStackFrame, extractFirst(line, STACK_FRAME_PATTERN));
        }

        // Python Agent 还没接管前，先生成一份 Java 侧兜底结果。
        // 如果 Python 调用失败，前端至少仍然能看到结构化诊断。
        diagnosis.setSimulatedReason(buildSimulatedReason(diagnosis));
        diagnosis.setPossibleReason(diagnosis.getSimulatedReason());
        diagnosis.setRootCause(diagnosis.getSimulatedReason());
        diagnosis.setConfidence(buildFallbackConfidence(diagnosis));
        diagnosis.setEvidenceLogs(buildEvidenceLogs(diagnosis));
        diagnosis.setRelatedFiles(buildRelatedFiles(diagnosis));
        diagnosis.setNextActions(List.of("检查结构化日志字段是否完整", "根据异常信息和首个业务堆栈定位对应实现"));
        diagnosis.setMissingInfo(buildMissingInfo(diagnosis, false));
        diagnosis.setNeedMoreInfo(!diagnosis.getMissingInfo().isEmpty());

        // Java 侧不主动产生人工核对项，人工核对项主要由 Python prompt 根据业务语义给出。
        diagnosis.setManualCheckItems(List.of());
        diagnosis.setAnalysisSource("JAVA_SIMULATION");
        return diagnosis;
    }

    /**
     * 调用 Python Agent 增强 Java 侧诊断结果。
     *
     * <p>这里采用“失败降级”策略：Python 未启用、未配置、调用失败、返回异常时，
     * 都保留 Java 侧模拟诊断结果，不让整个日志诊断接口失败。</p>
     */
    private void enrichDiagnosisByPython(LogDiagnosisRes diagnosis) {
        // Python Agent 可以通过配置开关关闭，关闭时直接使用 Java 侧兜底诊断。
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            return;
        }
        // 没有 baseUrl 说明 Python 服务未配置，同样走兜底。
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            return;
        }

        // Java 只把诊断必要上下文传给 Python：结构化字段 + 原始日志。
        PythonDiagnoseReq request = buildPythonDiagnoseReq(diagnosis);
        String url = buildPythonDiagnoseUrl();
        String requestJson = JSON.toJSONString(request);
        log.debug("Python 日志诊断请求: url={}, body={}", url, requestJson);

        HttpResponse response;
        try {
            // Hutool 这里使用同步 HTTP 调用。超时时间来自 yq.python.agent.timeoutMs。
            response = HttpRequest.post(url)
                    .body(requestJson, ContentType.JSON.toString())
                    .timeout(pythonAgentProperties.getTimeoutMs())
                    .execute();
        } catch (Exception ex) {
            log.error("调用 Python 日志诊断服务失败: url={}, body={}", url, requestJson, ex);
            diagnosis.setAnalysisSource("JAVA_SIMULATION");
            return;
        }

        String responseBody = response.body();
        log.debug("Python 日志诊断响应: url={}, status={}, body={}", url, response.getStatus(), responseBody);
        // 非 200 不合并 Python 结果，避免前端误以为是 AI 诊断。
        if (response.getStatus() != 200) {
            log.error("Python 日志诊断状态异常: url={}, status={}, body={}", url, response.getStatus(), responseBody);
            diagnosis.setAnalysisSource("JAVA_SIMULATION");
            return;
        }

        PythonDiagnoseRes pythonDiagnoseRes;
        try {
            // Python 返回字段来自模型 JSON，可能出现格式波动，所以解析独立封装并带容错。
            pythonDiagnoseRes = parsePythonDiagnoseRes(responseBody);
        } catch (Exception ex) {
            log.error("Python 日志诊断响应解析失败: url={}, body={}", url, responseBody, ex);
            diagnosis.setAnalysisSource("JAVA_SIMULATION");
            return;
        }

        if (pythonDiagnoseRes == null) {
            log.error("Python 日志诊断返回异常: url={}, body={}", url, responseBody);
            diagnosis.setAnalysisSource("JAVA_SIMULATION");
            return;
        }

        // 能走到这里，说明 Python Agent 返回可用结果，开始覆盖 Java 侧兜底字段。
        diagnosis.setAnalysisSource("PYTHON_AGENT");
        diagnosis.setAnalysisSummary(pythonDiagnoseRes.getSummary());

        // rootCause 优先，其次 possibleReason，最后保留 Java 兜底根因。
        diagnosis.setRootCause(firstNonBlank(pythonDiagnoseRes.getRootCause(), pythonDiagnoseRes.getPossibleReason(), diagnosis.getRootCause()));

        // Python 没给 confidence 时，保留 Java 侧按字段完整度计算的保守置信度。
        diagnosis.setConfidence(pythonDiagnoseRes.getConfidence() == null ? diagnosis.getConfidence() : pythonDiagnoseRes.getConfidence());
        diagnosis.setEvidenceLogs(pythonDiagnoseRes.getEvidenceLogs() == null ? List.of() : pythonDiagnoseRes.getEvidenceLogs());
        diagnosis.setRelatedFiles(pythonDiagnoseRes.getRelatedFiles() == null ? List.of() : pythonDiagnoseRes.getRelatedFiles());
        diagnosis.setNextActions(pythonDiagnoseRes.getNextActions() == null ? List.of() : pythonDiagnoseRes.getNextActions());
        diagnosis.setNeedMoreInfo(Boolean.TRUE.equals(pythonDiagnoseRes.getNeedMoreInfo()));
        diagnosis.setMissingInfo(pythonDiagnoseRes.getMissingInfo() == null ? List.of() : pythonDiagnoseRes.getMissingInfo());
        diagnosis.setManualCheckItems(pythonDiagnoseRes.getManualCheckItems() == null ? List.of() : pythonDiagnoseRes.getManualCheckItems());
        diagnosis.setRelatedCode(pythonDiagnoseRes.getRelatedCode() == null ? List.of() : pythonDiagnoseRes.getRelatedCode());
        diagnosis.setSuggestions(pythonDiagnoseRes.getSuggestions() == null ? List.of() : pythonDiagnoseRes.getSuggestions());
        if (StringUtils.hasText(pythonDiagnoseRes.getPossibleReason())) {
            // possibleReason 是前端早期展示字段，仍然保持同步。
            diagnosis.setPossibleReason(pythonDiagnoseRes.getPossibleReason());
        }
    }

    /**
     * 构造发给 Python Agent 的请求体。
     *
     * <p>只传诊断必要上下文：结构化字段和原始日志，不传 Java 内部配置和敏感凭证。</p>
     */
    private PythonDiagnoseReq buildPythonDiagnoseReq(LogDiagnosisRes diagnosis) {
        PythonDiagnoseReq request = new PythonDiagnoseReq();
        request.setGuid(diagnosis.getGuid());
        request.setRequestUri(diagnosis.getRequestUri());
        request.setHttpMethod(diagnosis.getHttpMethod());
        request.setController(diagnosis.getController());
        request.setRequestArgs(diagnosis.getRequestArgs());
        request.setExceptionClass(diagnosis.getExceptionClass());
        request.setExceptionMessage(diagnosis.getExceptionMessage());
        request.setTopStackFrame(diagnosis.getTopStackFrame());
        request.setRawLogs(diagnosis.getRawLogs());
        return request;
    }

    /**
     * 拼接 Python Agent 诊断接口地址。
     *
     * <p>兼容 baseUrl/path 是否带斜杠，避免配置成 127.0.0.1:9000/ 时出现双斜杠。</p>
     */
    private String buildPythonDiagnoseUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getDiagnosePath())
                ? pythonAgentProperties.getDiagnosePath().trim()
                : "/api/diagnose-log";
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    /**
     * 发送诊断进度事件。
     *
     * <p>普通接口调用时 progressConsumer 为空，直接跳过。</p>
     */
    private void emit(Consumer<LogDiagnosisStreamEvent> progressConsumer, String stage, String status, String message, Object data) {
        if (progressConsumer == null) {
            return;
        }
        LogDiagnosisStreamEvent event = new LogDiagnosisStreamEvent();
        event.setStage(stage);
        event.setStatus(status);
        event.setMessage(message);
        event.setData(data);
        event.setTimestamp(System.currentTimeMillis());
        progressConsumer.accept(event);
    }

    /**
     * 解析 Python Agent 返回结果。
     *
     * <p>Python 可能直接返回诊断 JSON，也可能被包装成 ApiResponse 风格的
     * {"code": 200, "data": {...}}。这里同时兼容两种格式。</p>
     */
    private PythonDiagnoseRes parsePythonDiagnoseRes(String responseBody) {
        JSONObject root = JSON.parseObject(responseBody);
        if (root == null || root.isEmpty()) {
            return null;
        }

        // Python FastAPI 当前是直接返回诊断 JSON。
        // 这里同时兼容 Java ApiResponse 风格包装，方便以后统一网关或包装响应。
        JSONObject data = root;
        Integer code = root.getInteger("code");
        if (code != null || root.containsKey("data")) {
            if (code == null || code != 200) {
                return null;
            }
            data = root.getJSONObject("data");
        }

        if (data == null || data.isEmpty()) {
            return null;
        }

        PythonDiagnoseRes result = new PythonDiagnoseRes();

        // 字段名同时兼容 snake_case 和 camelCase：
        // Python 内部偏 snake_case，Java/前端偏 camelCase，模型偶尔也会混用。
        result.setGuid(firstNonBlank(data.getString("guid")));
        result.setSummary(firstNonBlank(data.getString("summary"), data.getString("analysis_summary")));
        result.setPossibleReason(firstNonBlank(data.getString("possibleReason"), data.getString("possible_reason")));
        result.setRootCause(firstNonBlank(data.getString("rootCause"), data.getString("root_cause")));
        result.setConfidence(parseConfidence(data.get("confidence")));
        result.setEvidenceLogs(parseStringList(data.get("evidenceLogs"), data.get("evidence_logs")));
        result.setRelatedFiles(parseStringList(data.get("relatedFiles"), data.get("related_files")));
        result.setNextActions(parseStringList(data.get("nextActions"), data.get("next_actions")));
        result.setNeedMoreInfo(parseBoolean(data.get("needMoreInfo"), data.get("need_more_info")));
        result.setMissingInfo(parseStringList(data.get("missingInfo"), data.get("missing_info")));
        result.setManualCheckItems(parseStringList(data.get("manualCheckItems"), data.get("manual_check_items")));
        result.setRelatedCode(parseStringList(data.get("relatedCode"), data.get("related_code")));
        result.setSuggestions(parseStringList(data.get("suggestions")));
        return result;
    }

    /**
     * 兼容解析字符串数组字段。
     *
     * <p>模型输出不一定稳定：有时是数组，有时是单个字符串，有时是 JSON 数组字符串。
     * Java 侧统一转换为 List&lt;String&gt;，避免前端处理多种类型。</p>
     */
    private List<String> parseStringList(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> list) {
                // 标准情况：Python/FastAPI 正常返回 JSON 数组。
                return list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                try {
                    // 兼容模型把数组作为字符串输出的情况，例如 "[\"a\", \"b\"]"。
                    return JSON.parseArray(text, String.class);
                } catch (Exception ignored) {
                    // 如果不是 JSON 数组字符串，就把它当作单条内容。
                    return List.of(text);
                }
            }
        }
        return List.of();
    }

    /**
     * 解析并约束置信度范围。
     *
     * <p>模型可能返回字符串数字或越界数值，这里统一压到 0~1。</p>
     */
    private Double parseConfidence(Object value) {
        if (value == null) {
            return null;
        }
        try {
            double confidence = Double.parseDouble(String.valueOf(value));
            return Math.max(0D, Math.min(1D, confidence));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 兼容解析布尔字段。
     *
     * <p>Python/模型可能返回 Boolean，也可能返回字符串 "true"/"false"。</p>
     */
    private Boolean parseBoolean(Object... values) {
        for (Object value : values) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                return Boolean.parseBoolean(text);
            }
        }
        return false;
    }

    /**
     * 构造 Java 侧兜底诊断文案。
     *
     * <p>当 Python Agent 不可用时，至少能把异常信息、异常类型、controller、
     * 首个堆栈这些结构化结果解释给前端。</p>
     */
    private String buildSimulatedReason(LogDiagnosisRes diagnosis) {
        if (StringUtils.hasText(diagnosis.getExceptionMessage())) {
            // 有明确异常信息时，把已提取到的结构化字段拼成一段可读诊断。
            StringBuilder builder = new StringBuilder("模拟诊断：");
            builder.append("本次 guid 对应的异常信息为“").append(diagnosis.getExceptionMessage()).append("”");
            if (StringUtils.hasText(diagnosis.getExceptionClass())) {
                builder.append("，异常类型为 ").append(diagnosis.getExceptionClass());
            }
            if (StringUtils.hasText(diagnosis.getController())) {
                builder.append("，相关控制器为 ").append(diagnosis.getController());
            }
            if (StringUtils.hasText(diagnosis.getTopStackFrame())) {
                builder.append("，首个堆栈位置为 ").append(diagnosis.getTopStackFrame());
            }
            builder.append("。后续可将这些结构化字段交给 Python/LangChain 进一步分析根因。");
            return builder.toString();
        }
        // 没有异常信息时，不强行猜根因，只提示当前结构化信息不足。
        return "模拟诊断：当前 guid 已查到日志，但还没有稳定提取到明确异常信息。后续可由 Python 链路继续结合日志正文和知识库做推理。";
    }

    /**
     * 把火山 TLS SDK 的日志 Map 转成统一日志记录。
     */
    private List<LogRecordRes> parseLogs(List<Map<String, Object>> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        return logs.stream()
                .filter(Objects::nonNull)
                .map(item -> convertLogRecord((JSONObject) JSON.toJSON(item)))
                .toList();
    }

    /**
     * Java 侧兜底置信度。
     *
     * <p>这个分数只基于结构化字段完整度，比较保守，避免 Python 不可用时展示过高可信度。</p>
     */
    private Double buildFallbackConfidence(LogDiagnosisRes diagnosis) {
        // 基础分代表“已经按 guid 找到日志，但还没有足够证据”。
        double confidence = 0.35D;
        if (StringUtils.hasText(diagnosis.getExceptionMessage())) {
            // 有异常信息，比只有日志正文更可靠。
            confidence += 0.15D;
        }
        if (StringUtils.hasText(diagnosis.getTopStackFrame())) {
            // 有业务堆栈，说明已经能定位到代码位置。
            confidence += 0.15D;
        }
        if (StringUtils.hasText(diagnosis.getController())) {
            // 有入口 controller，排查范围进一步收窄。
            confidence += 0.1D;
        }
        // Java 侧兜底诊断不经过 RAG/LLM，最高限制在 0.7。
        return Math.min(confidence, 0.7D);
    }

    /**
     * Java 侧兜底证据列表。
     *
     * <p>优先使用异常信息、异常类型、首个堆栈；如果都没有，再截取第一条原始日志。</p>
     */
    private List<String> buildEvidenceLogs(LogDiagnosisRes diagnosis) {
        List<String> evidence = new java.util.ArrayList<>();
        if (StringUtils.hasText(diagnosis.getExceptionMessage())) {
            evidence.add("异常信息: " + diagnosis.getExceptionMessage());
        }
        if (StringUtils.hasText(diagnosis.getExceptionClass())) {
            evidence.add("异常类型: " + diagnosis.getExceptionClass());
        }
        if (StringUtils.hasText(diagnosis.getTopStackFrame())) {
            evidence.add("首个堆栈: " + diagnosis.getTopStackFrame());
        }
        if (evidence.isEmpty() && diagnosis.getRawLogs() != null && !diagnosis.getRawLogs().isEmpty()) {
            String rawLog = diagnosis.getRawLogs().get(0);
            evidence.add(rawLog.length() > 500 ? rawLog.substring(0, 500) + "...(truncated)" : rawLog);
        }
        return evidence;
    }

    /**
     * Java 侧兜底相关位置。
     */
    private List<String> buildRelatedFiles(LogDiagnosisRes diagnosis) {
        List<String> relatedFiles = new java.util.ArrayList<>();
        if (StringUtils.hasText(diagnosis.getController())) {
            relatedFiles.add(diagnosis.getController());
        }
        if (StringUtils.hasText(diagnosis.getTopStackFrame())) {
            relatedFiles.add(diagnosis.getTopStackFrame());
        }
        return relatedFiles;
    }

    /**
     * Java 侧兜底缺失信息。
     *
     * <p>这里的 missingInfo 只放后续二次 RAG 能从代码知识库继续检索的代码资料。
     * 请求参数、请求头、数据库状态等运行时信息应该进入人工核对项，不放这里。</p>
     */
    private List<String> buildMissingInfo(LogDiagnosisRes diagnosis, boolean hasKnowledgeContext) {
        List<String> missingInfo = new java.util.ArrayList<>();
        // 下面这些缺失项都必须能被代码知识库继续检索。
        if (!StringUtils.hasText(diagnosis.getTopStackFrame())) {
            missingInfo.add("异常发生位置附近的 Service/Controller 方法源码");
        }
        if (!StringUtils.hasText(diagnosis.getController())) {
            missingInfo.add("触发异常的 Controller 方法源码");
        }
        if (!hasKnowledgeContext) {
            missingInfo.add("知识库中相关 Service、Controller、Interceptor、Mapper 或异常处理器代码");
        }
        return missingInfo;
    }

    /**
     * 转换单条火山日志。
     *
     * <p>优先读取火山标准字段 __time__/__content__，否则保留完整 JSON 字符串作为正文。</p>
     */
    private LogRecordRes convertLogRecord(JSONObject logJson) {
        LogRecordRes record = new LogRecordRes();
        record.setTime(firstNonBlank(
                logJson.getString("time"),
                logJson.getString("__time__"),
                logJson.getString("timestamp")
        ));
        record.setContent(firstNonBlank(
                logJson.getString("__content__"),
                logJson.getString("content"),
                JSON.toJSONString(logJson)
        ));
        return record;
    }

    /**
     * 解析 topicId。
     *
     * <p>前端默认不传 topicId，后端从系统动态配置中读取默认 topic。</p>
     */
    private String resolveTopicId(String requestTopicId) {
        String topicId = StringUtils.hasText(requestTopicId)
                ? requestTopicId.trim()
                : tlsDynamicProperties.getDefaultTopicId();
        if (!StringUtils.hasText(topicId)) {
            throw new BusinessException(400, "topicId 不能为空，且系统配置中未设置默认 topicId");
        }
        return topicId;
    }

    /**
     * 把 Java Date 转成火山 TLS SDK 需要的 Unix 秒。
     */
    private long toEpochSeconds(Date value) {
        if (value == null) {
            throw new BusinessException(400, "时间不能为空");
        }
        return value.getTime() / 1000;
    }

    /**
     * 校验火山 TLS 基础配置。
     */
    private void validateConfig() {
        if (!StringUtils.hasText(tlsProperties.getEndpoint())
                || !StringUtils.hasText(tlsProperties.getAccessKeyId())
                || !StringUtils.hasText(tlsProperties.getAccessKeySecret())) {
            throw new BusinessException(500, "火山日志服务配置不完整，请检查 endpoint 和 AK/SK");
        }
    }

    /**
     * 返回第一个非空字符串。
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 用正则提取第一个分组。
     */
    private String extractFirst(String source, Pattern pattern) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        Matcher matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    /**
     * 仅在目标字段为空时填充。
     *
     * <p>同一个 guid 可能查到多条日志，先出现的关键字段通常更接近异常现场，
     * 因此这里不覆盖已提取到的值。</p>
     */
    private void fillIfAbsent(ValueSupplier getter, ValueConsumer setter, String value) {
        if (!StringUtils.hasText(getter.get()) && StringUtils.hasText(value)) {
            setter.accept(value);
        }
    }

    @FunctionalInterface
    private interface ValueSupplier {
        String get();
    }

    @FunctionalInterface
    private interface ValueConsumer {
        void accept(String value);
    }
}
