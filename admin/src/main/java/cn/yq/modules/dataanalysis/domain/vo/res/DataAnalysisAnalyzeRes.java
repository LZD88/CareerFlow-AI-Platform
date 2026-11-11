package cn.yq.modules.dataanalysis.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据分析完整响应。
 *
 * <p>该响应聚合了 Python 生成的 SQL 计划、Java 查询结果和 Python 结果解释。
 * 前端可以用 rows/columns 渲染表格，用 analysis/insights 展示 AI 解读。</p>
 */
@Data
public class DataAnalysisAnalyzeRes {

    /**
     * 用户原始问题。
     */
    private String question;

    /**
     * Python 选表阶段判断本次分析需要使用的候选表。
     */
    private List<String> candidateTables = new ArrayList<>();

    /**
     * Java 校验后实际执行的 SQL。
     *
     * <p>该 SQL 已经过 Java SQL Guard 处理，可能和 Python 原始 SQL 不完全一致，
     * 例如 Java 可能追加或收紧 LIMIT。</p>
     */
    private String sql;

    /**
     * Python 生成 SQL 时给出的原因说明。
     */
    private String reason;

    /**
     * 是否需要用户先补充问题条件。
     *
     * <p>如果为 true，一般不会生成和执行 SQL。</p>
     */
    private Boolean needsClarification = Boolean.FALSE;

    /**
     * 需要用户补充回答的澄清问题。
     */
    private List<String> clarificationQuestions = new ArrayList<>();

    /**
     * 是否缺少系统侧知识库信息。
     *
     * <p>如果为 true，一般需要管理员补充 schema、枚举或指标口径后再分析。</p>
     */
    private Boolean knowledgeMissing = Boolean.FALSE;

    /**
     * 缺少的知识项。
     */
    private List<String> knowledgeMissingItems = new ArrayList<>();

    /**
     * 查询结果列名。
     */
    private List<String> columns = new ArrayList<>();

    /**
     * 查询结果行数据。
     */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /**
     * 当前返回的行数。
     */
    private Integer rowCount = 0;

    /**
     * 结果是否可能被 maxRows/LIMIT 截断。
     */
    private Boolean truncated = Boolean.FALSE;

    /**
     * Python 对查询结果生成的整体分析结论。
     */
    private String analysis;

    /**
     * Python 提炼出的关键洞察。
     */
    private List<String> insights = new ArrayList<>();

    /**
     * Python 推荐的后续追问方向。
     */
    private List<String> followUpQuestions = new ArrayList<>();

    /**
     * Python 推荐的图表展示方式。
     */
    private PythonDataAnalysisInterpretResultRes.ChartSuggestion chartSuggestion;

    /**
     * SQL 生成阶段采用的口径假设。
     */
    private List<String> assumptions = new ArrayList<>();

    /**
     * SQL 生成、SQL 执行或结果解释阶段产生的提示与风险信息。
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * 候选表中缺少 DDL 的表。
     *
     * <p>如果该字段非空，一般不会执行 SQL。</p>
     */
    private List<String> missingTables = new ArrayList<>();
}
