package cn.yq.modules.dataanalysis.domain.vo.res;

import lombok.Data;

import java.util.List;

/**
 * Python 数据分析表结构召回响应。
 */
@Data
public class DataAnalysisSchemaRes {

    /**
     * 已召回的表结构列表。
     */
    private List<SchemaItem> schemas;

    /**
     * 未能从 Milvus 取回 DDL 的表。
     */
    private List<String> missingTables;

    /**
     * 单张表的 DDL 文档。
     */
    @Data
    public static class SchemaItem {
        /**
         * 表名。
         */
        private String table;
        /**
         * 文档标题，一般等于表名。
         */
        private String title;
        /**
         * 表结构 Markdown/DDL 内容。
         */
        private String content;
        /**
         * Milvus 中的业务文档 ID。
         */
        private String documentId;
        /**
         * Milvus chunk ID。
         */
        private String chunkId;
        /**
         * 文档版本。
         */
        private Integer version;
    }
}
