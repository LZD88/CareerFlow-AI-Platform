package cn.yq.modules.knowledge.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 知识库文档元数据。
 *
 * <p>MySQL 负责保存业务状态和原始文本，Milvus 负责保存 chunk 向量和检索字段。</p>
 */
@Data
public class KnowledgeDocumentEntity {

    private Long id;
    private String documentCode;
    private String title;
    private String content;
    private String sourceType;
    private String sourcePath;
    private Long courseId;
    private Long classId;
    private String studyMode;
    private Integer version;
    private String permissionScope;
    private String status;
    private Integer chunkCount;
    private String errorMessage;
    private Date indexedAt;
    private Date createdAt;
    private Date updatedAt;
}
