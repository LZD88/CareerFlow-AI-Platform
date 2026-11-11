package cn.yq.modules.dataanalysis.domain.vo.req;

import lombok.Data;

/**
 * Java 调 Python Agent 生成 SQL 的内部请求。
 *
 * <p>Python 只负责根据问题、表目录和 Milvus 中的 DDL 生成 SQL，
 * 不直接查询业务数据库。</p>
 */
@Data
public class PythonDataAnalysisGenerateSqlReq {

    /**
     * 用户自然语言问题。
     */
    private String question;

    /**
     * 期望 SQL 最大返回行数。
     */
    private Integer maxRows;
}
