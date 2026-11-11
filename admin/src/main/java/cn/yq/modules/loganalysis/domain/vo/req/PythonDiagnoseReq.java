package cn.yq.modules.loganalysis.domain.vo.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Java 后端传给 Python Agent 的诊断上下文。
 *
 * <p>Java 负责查火山日志并做第一层结构化提取，Python Agent 只需要基于这些
 * 结构化字段、原始日志和知识库内容做 RAG + LLM 分析。</p>
 */
@Data
public class PythonDiagnoseReq {

    /**
     * 请求唯一标识，用于串起 Java 日志查询结果和 Python 诊断结果。
     */
    private String guid;

    /**
     * 请求路径。
     */
    private String requestUri;

    /**
     * HTTP 方法。
     */
    private String httpMethod;

    /**
     * Java controller#method。
     */
    private String controller;

    /**
     * ControllerLogAspect 从接口开始日志中记录的请求参数。
     *
     * <p>通常是已经脱敏后的 JSON 字符串，例如 [{"name":"request","value":{...}}]。
     * Java 只透传日志里已有的信息，不额外查询敏感数据。</p>
     */
    private String requestArgs;

    /**
     * 异常类型。
     */
    private String exceptionClass;

    /**
     * 异常信息。
     */
    private String exceptionMessage;

    /**
     * 首个关键堆栈行。
     */
    private String topStackFrame;

    /**
     * guid 命中的原始日志正文。
     */
    private List<String> rawLogs = new ArrayList<>();
}
