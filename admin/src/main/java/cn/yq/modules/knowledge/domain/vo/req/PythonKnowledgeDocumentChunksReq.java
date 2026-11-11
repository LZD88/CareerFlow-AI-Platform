package cn.yq.modules.knowledge.domain.vo.req;

import lombok.Data;

/**
 * Java 后端调用 Python Agent 查询 Milvus chunk 的请求体。
 *
 * <p>前端只传知识文档 id，Java 会先查 MySQL 得到 documentCode 和 version，
 * 再组装这个内部请求，避免前端任意构造 documentId 查询其他向量数据。</p>
 */
@Data
public class PythonKnowledgeDocumentChunksReq {

    /**
     * 业务文档编码，对应 knowledge_document.document_code。
     */
    private String documentId;

    /**
     * 文档版本。当前默认使用 MySQL 中该文档的最新 version。
     */
    private Integer version;

    /**
     * 知识域：STUDENT_QA / LOG_DIAGNOSIS。
     *
     * <p>Python 侧会据此选择不同 Milvus collection。</p>
     */
    private String knowledgeDomain;

    /**
     * 页码，从 1 开始。
     */
    private Integer pageNum;

    /**
     * 每页数量。
     */
    private Integer pageSize;

    /**
     * 是否返回完整向量。默认 false，仅返回维度和少量预览值。
     */
    private Boolean includeVector;
}
