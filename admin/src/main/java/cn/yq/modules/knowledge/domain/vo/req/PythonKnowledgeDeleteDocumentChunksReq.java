package cn.yq.modules.knowledge.domain.vo.req;

import lombok.Data;

/**
 * Java 后端调用 Python Agent 删除 Milvus 文档 chunk 的请求体。
 */
@Data
public class PythonKnowledgeDeleteDocumentChunksReq {

    /**
     * 业务文档编码，对应 knowledge_document.document_code。
     */
    private String documentId;

    /**
     * 要删除的文档版本。不传时 Python 会删除该文档全部版本。
     */
    private Integer version;

    /**
     * 知识域：STUDENT_QA / LOG_DIAGNOSIS。
     *
     * <p>Python 侧会据此选择不同 Milvus collection。</p>
     */
    private String knowledgeDomain;
}
