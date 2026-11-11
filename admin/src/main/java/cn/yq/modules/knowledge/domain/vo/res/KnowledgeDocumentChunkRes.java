package cn.yq.modules.knowledge.domain.vo.res;

import lombok.Data;

import java.util.List;

/**
 * 知识文档在 Milvus 中的单个 chunk 展示对象。
 *
 * <p>字段基本来自 Milvus dynamic field。后台页面用它查看切片正文、章节路径、
 * 课程范围和向量维度，便于排查 RAG 入库质量。</p>
 */
@Data
public class KnowledgeDocumentChunkRes {

    /**
     * Milvus 主键，例如 doc_kd_xxx_v1_chunk_0001。
     */
    private String id;

    /**
     * 业务文档编码，对应 knowledge_document.document_code。
     */
    private String documentId;

    /**
     * 文档内 chunk 序号，例如 chunk_0001。
     */
    private String chunkId;

    /**
     * 适用课程 ID；0 表示全局资料。
     */
    private Long courseId;

    /**
     * 适用班级 ID；当前第一版通常为 0 或空。
     */
    private Long classId;

    /**
     * 适用学员范围：ALL / ONLINE / OFFLINE。
     */
    private String studyMode;

    /**
     * 权限范围标记，第一版先作为元数据保留。
     */
    private String permissionScope;

    /**
     * chunk 所属文档标题。
     */
    private String title;

    /**
     * Markdown 标题路径，例如 缴费说明 > 付款方式。
     */
    private String headerPath;

    /**
     * chunk 原文内容。
     */
    private String content;

    /**
     * chunk 原文长度，方便快速判断切片大小是否合理。
     */
    private Integer contentLength;

    /**
     * 文档入库版本号。
     */
    private Integer version;

    /**
     * 来源类型，例如 manual / markdown / txt。
     */
    private String sourceType;

    /**
     * 来源路径，例如 docs/payment.md。
     */
    private String sourcePath;

    /**
     * 数据分析表结构文档对应的表名。
     */
    private String tableName;

    /**
     * 向量维度，例如 1024。
     */
    private Integer vectorDim;

    /**
     * 向量前几位预览。页面默认只展示预览，避免传输完整大向量。
     */
    private List<Double> vectorPreview;

    /**
     * 完整向量。仅 includeVector=true 时返回，排查时才建议打开。
     */
    private List<Double> vector;
}
