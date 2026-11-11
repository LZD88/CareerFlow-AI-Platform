package cn.yq.modules.knowledge.domain.vo.req;

import lombok.Data;

/**
 * Java 后端提交给 Python Agent 的知识文档入库请求。
 *
 * <p>Java 负责保存文档元数据和状态，Python 负责切片、embedding 和 Milvus 写入。</p>
 */
@Data
public class PythonKnowledgeIngestTextReq {

    /**
     * 业务文档 ID，对应 knowledge_document.document_code。
     */
    private String documentId;

    /**
     * 文档标题，Python 会作为 chunk 元数据写入 Milvus。
     */
    private String title;

    /**
     * 原始文档正文。
     */
    private String content;

    /**
     * 课程 ID。学生问答知识库用于按课程过滤；代码知识库通常为空。
     */
    private Long courseId;

    /**
     * 班级 ID，当前第一版保留字段。
     */
    private Long classId;

    /**
     * 学习范围：ALL / ONLINE / OFFLINE。代码知识库默认 ALL。
     */
    private String studyMode;

    /**
     * 文档版本。重新入库时 Java 侧会递增版本，并在成功后清理旧版本 chunk。
     */
    private Integer version;

    /**
     * 来源类型，例如 manual / markdown / generated_java_md。
     *
     * <p>generated_java_md 会让 Python 按 Java 方法段落切片。</p>
     */
    private String sourceType;

    /**
     * 来源路径或上传文件名。
     */
    private String sourcePath;

    /**
     * 权限范围或业务域标记，例如 course / log_diagnosis。
     */
    private String permissionScope;

    /**
     * 知识域：STUDENT_QA / LOG_DIAGNOSIS。
     *
     * <p>Python 会把它写入 Milvus 动态字段，便于后续检索链路隔离。</p>
     */
    private String knowledgeDomain;
}
