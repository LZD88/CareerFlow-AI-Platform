package cn.yq.modules.dataanalysis.domain.vo.res;

import lombok.Data;

import java.util.List;

/**
 * 数据分析选表响应。
 *
 * <p>当前 Python /plan 只返回 candidateTables 和 reason；
 * schema 相关字段保留用于兼容早期接口和调试场景。</p>
 */
@Data
public class DataAnalysisPlanRes {

    /**
     * 候选表列表。
     */
    private List<String> candidateTables;

    /**
     * 选表原因。
     */
    private String reason;

    /**
     * 兼容字段：早期 Python 返回是否需要继续取 schema。
     */
    private Boolean needSchema;

    /**
     * 兼容字段：早期 Python 直接返回的表结构。
     */
    private List<DataAnalysisSchemaRes.SchemaItem> schemas;

    /**
     * 兼容字段：缺少 DDL 的表。
     */
    private List<String> missingTables;
}
