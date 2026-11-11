package cn.yq.modules.dataanalysis.domain.vo.res;

import lombok.Data;

import java.util.List;

/**
 * Python Agent 选表响应。
 */
@Data
public class PythonDataAnalysisPlanRes {

    /**
     * 候选表列表。
     */
    private List<String> candidateTables;

    /**
     * 选表原因。
     */
    private String reason;

    /**
     * 兼容旧版 Python 字段。
     */
    private Boolean needSchema;

    /**
     * 兼容旧版 Python 字段。
     */
    private List<DataAnalysisSchemaRes.SchemaItem> schemas;

    /**
     * 兼容旧版 Python 字段。
     */
    private List<String> missingTables;
}
