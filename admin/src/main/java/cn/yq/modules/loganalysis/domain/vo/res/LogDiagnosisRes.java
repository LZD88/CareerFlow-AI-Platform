package cn.yq.modules.loganalysis.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志诊断最终返回给前端的结果。
 *
 * <p>这个对象会合并两部分信息：</p>
 * <p>1. Java 侧从火山日志查询和正则结构化提取得到的请求、异常、堆栈信息。</p>
 * <p>2. Python Agent 侧基于 RAG + LLM 给出的根因、证据、置信度和排查建议。</p>
 */
@Data
public class LogDiagnosisRes {

    /**
     * 请求唯一标识。前端按 guid 发起诊断，Java 也用它去火山日志检索同一请求链路。
     */
    private String guid;

    /**
     * 实际用于查询火山日志的检索语句。按 guid 诊断时通常就是 guid 本身。
     */
    private String query;

    /**
     * 日志查询开始时间，Unix 秒。
     */
    private Long startTime;

    /**
     * 日志查询结束时间，Unix 秒。
     */
    private Long endTime;

    /**
     * 从日志中提取到的请求路径。
     */
    private String requestUri;

    /**
     * 从日志中提取到的 HTTP 方法。
     */
    private String httpMethod;

    /**
     * 从 controller 统一日志中提取到的 controller#method。
     */
    private String controller;

    /**
     * 从接口开始日志中提取到的请求参数。
     *
     * <p>该字段来自 ControllerLogAspect 的 args，已按日志侧规则脱敏和截断。</p>
     */
    private String requestArgs;

    /**
     * 从异常日志中提取到的异常类型。
     */
    private String exceptionClass;

    /**
     * 从异常日志中提取到的异常信息。
     */
    private String exceptionMessage;

    /**
     * 首个关键堆栈行，用来帮助定位最可能的业务代码位置。
     */
    private String topStackFrame;

    /**
     * Java 侧兜底模拟诊断结果。
     *
     * <p>当 Python Agent 未启用或调用失败时，前端仍可展示这个保底说明。</p>
     */
    private String simulatedReason;

    /**
     * 最可能原因。Python 返回时会覆盖 Java 模拟结果。
     */
    private String possibleReason;

    /**
     * 根因判断，比 possibleReason 更偏最终结论。
     */
    private String rootCause;

    /**
     * 诊断可信度，范围 0~1。
     *
     * <p>这是模型基于当前证据给出的可信程度，不代表真实准确率。</p>
     */
    private Double confidence;

    /**
     * 支撑 rootCause 的证据片段。
     */
    private List<String> evidenceLogs = new ArrayList<>();

    /**
     * 相关文件、类、方法或堆栈位置。
     */
    private List<String> relatedFiles = new ArrayList<>();

    /**
     * 下一步排查动作。
     */
    private List<String> nextActions = new ArrayList<>();

    /**
     * 是否还需要补充信息才能更有把握地下结论。
     */
    private Boolean needMoreInfo = false;

    /**
     * 系统后续可以自动补查的信息，例如代码、非敏感日志、配置名。
     */
    private List<String> missingInfo = new ArrayList<>();

    /**
     * 需要人工在安全环境中核对的事项，例如数据库状态、权限绑定、账号状态。
     */
    private List<String> manualCheckItems = new ArrayList<>();

    /**
     * 分析来源。常见值：JAVA_SIMULATION、PYTHON_AGENT。
     */
    private String analysisSource;

    /**
     * Python Agent 返回的一句话分析摘要。
     */
    private String analysisSummary;

    /**
     * 兼容早期字段：相关类或方法。
     */
    private List<String> relatedCode = new ArrayList<>();

    /**
     * 兼容早期字段：排查建议。
     */
    private List<String> suggestions = new ArrayList<>();

    /**
     * 按 guid 命中的日志条数。
     */
    private Integer matchedLogCount;

    /**
     * 原始日志正文，用于前端展示和人工复核。
     */
    private List<String> rawLogs = new ArrayList<>();
}
