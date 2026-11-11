package cn.yq.modules.loganalysis.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Python 日志诊断服务返回结果。
 *
 * <p>这不是直接返回给前端的最终对象，而是 Java 后端调用
 * YQ-AI-PY-AGENT 后拿到的中间结果。Java 会把这些字段合并到
 * {@link LogDiagnosisRes}，同时保留 Java 侧查询日志、结构化提取出来的字段。</p>
 */
@Data
public class PythonDiagnoseRes {

    /**
     * 本次诊断对应的请求 guid，用于和 Java 侧日志查询结果对齐。
     */
    private String guid;

    /**
     * Python/LLM 对本次异常的一句话摘要。
     */
    private String summary;

    /**
     * 最可能的原因，偏自然语言说明。
     */
    private String possibleReason;

    /**
     * 更明确的根因判断。
     *
     * <p>如果证据不足，Python prompt 会要求模型明确说明这是初步判断。</p>
     */
    private String rootCause;

    /**
     * 模型基于当前证据给出的诊断可信度，范围 0~1。
     *
     * <p>这是“诊断可信度”，不是线上真实准确率。</p>
     */
    private Double confidence;

    /**
     * 支撑诊断结论的证据片段。
     *
     * <p>来源通常包括原始日志、结构化异常字段、知识库召回内容。</p>
     */
    private List<String> evidenceLogs = new ArrayList<>();

    /**
     * 相关代码文件、类、方法或堆栈位置。
     */
    private List<String> relatedFiles = new ArrayList<>();

    /**
     * 建议研发接下来执行的排查动作。
     */
    private List<String> nextActions = new ArrayList<>();

    /**
     * 当前证据是否还不足以支撑明确根因。
     */
    private Boolean needMoreInfo;

    /**
     * 系统后续可以自动补查的信息。
     *
     * <p>例如相关代码片段、非敏感日志、配置名、上游响应摘要。
     * 敏感数据和生产数据库状态不要放在这里。</p>
     */
    private List<String> missingInfo = new ArrayList<>();

    /**
     * 需要研发或运维在安全环境中人工核对的事项。
     *
     * <p>例如用户是否存在、密码摘要是否匹配、权限绑定是否正确。
     * 这些信息不应该自动交给模型或自动查询生产库。</p>
     */
    private List<String> manualCheckItems = new ArrayList<>();

    /**
     * 兼容早期字段：相关类或方法。
     *
     * <p>新页面优先使用 relatedFiles，但这个字段保留给旧逻辑和降级展示。</p>
     */
    private List<String> relatedCode = new ArrayList<>();

    /**
     * 兼容早期字段：排查建议。
     *
     * <p>新页面优先使用 nextActions，但这个字段保留给旧逻辑和降级展示。</p>
     */
    private List<String> suggestions = new ArrayList<>();
}
