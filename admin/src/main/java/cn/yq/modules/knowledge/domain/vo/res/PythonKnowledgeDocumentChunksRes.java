package cn.yq.modules.knowledge.domain.vo.res;

import lombok.Data;

import java.util.List;

/**
 * Python Agent 查询 Milvus 文档 chunk 明细的响应体。
 *
 * <p>这个类只用于 Java 后端和 Python Agent 之间的内部调用，不直接表达数据库表结构。
 * Java 管理端对外仍然统一包装成 {@code ApiResponse<PageResult<KnowledgeDocumentChunkRes>>}。</p>
 */
@Data
public class PythonKnowledgeDocumentChunksRes {

    /**
     * Python 接口是否处理成功。
     */
    private Boolean ok;

    /**
     * 业务文档编码，对应 knowledge_document.document_code。
     */
    private String documentId;

    /**
     * 查询的文档版本；当前页面默认只查 MySQL 中记录的最新 version。
     */
    private Integer version;

    /**
     * Milvus collection 名称，方便页面或日志确认数据来源。
     */
    private String collection;

    /**
     * 当前过滤条件下的 chunk 总数。
     */
    private Long total;

    /**
     * Python 实际处理的页码。
     */
    private Integer pageNum;

    /**
     * Python 实际处理的每页数量。
     */
    private Integer pageSize;

    /**
     * 当前页 chunk 明细。
     */
    private List<KnowledgeDocumentChunkRes> records;
}
