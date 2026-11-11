package cn.yq.modules.dataanalysis.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Python Agent 生成 SQL 的响应。
 *
 * <p>这是 Java 和 Python 之间的内部协议。Java 收到后还会做 SQL Guard，
 * 因此这里的 sql 不能直接执行。</p>
 */
@Data
public class PythonDataAnalysisGenerateSqlRes {

    /**
     * 用户原始问题。
     */
    private String question;

    /**
     * Python 选出的候选表。
     */
    private List<String> candidateTables = new ArrayList<>();

    /**
     * Python 生成的 SQL，执行前必须经过 Java SQL Guard。
     */
    private String sql;

    /**
     * SQL 生成原因。
     */
    private String reason;

    /**
     * 是否需要用户先补充问题条件。
     */
    private Boolean needsClarification = Boolean.FALSE;

    /**
     * 需要用户补充回答的澄清问题。
     */
    private List<String> clarificationQuestions = new ArrayList<>();

    /**
     * 是否缺少系统侧知识库信息。
     */
    private Boolean knowledgeMissing = Boolean.FALSE;

    /**
     * 缺少的知识项。
     */
    private List<String> knowledgeMissingItems = new ArrayList<>();

    /**
     * 生成 SQL 时采用的口径假设。
     */
    private List<String> assumptions = new ArrayList<>();

    /**
     * Python 生成阶段的风险提示。
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * 缺少 DDL 的候选表。
     */
    private List<String> missingTables = new ArrayList<>();
}
