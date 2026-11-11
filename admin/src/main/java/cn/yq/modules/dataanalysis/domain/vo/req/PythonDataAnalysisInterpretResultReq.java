package cn.yq.modules.dataanalysis.domain.vo.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java 调 Python Agent 解释查询结果的内部请求。
 *
 * <p>Java 已经完成 SQL 执行，只把查询结果样本和上下文交给 Python 生成自然语言分析。</p>
 */
@Data
public class PythonDataAnalysisInterpretResultReq {

    /**
     * 用户原始问题。
     */
    private String question;

    /**
     * Java 实际执行的 SQL。
     */
    private String sql;

    /**
     * 查询结果列名。
     */
    private List<String> columns = new ArrayList<>();

    /**
     * 查询结果行。
     *
     * <p>第一版直接传 Java 返回给前端的结果集。后续如果明细很多，可以只传前 N 行样本。</p>
     */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /**
     * 返回给 Python 的行数。
     */
    private Integer rowCount = 0;

    /**
     * 结果是否可能被 LIMIT 截断。
     */
    private Boolean truncated = Boolean.FALSE;

    /**
     * SQL 生成阶段模型做出的口径假设。
     */
    private List<String> assumptions = new ArrayList<>();

    /**
     * SQL 生成或查询阶段产生的提示与风险信息。
     */
    private List<String> warnings = new ArrayList<>();
}
