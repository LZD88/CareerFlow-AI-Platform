package cn.yq.modules.knowledge.domain.vo.res;

import lombok.Data;

/**
 * Python Agent 删除 Milvus 文档 chunk 的响应体。
 */
@Data
public class PythonKnowledgeDeleteDocumentChunksRes {

    private Boolean ok;
    private String documentId;
    private Integer version;
    private String collection;
    private Integer deletedCount;
}
