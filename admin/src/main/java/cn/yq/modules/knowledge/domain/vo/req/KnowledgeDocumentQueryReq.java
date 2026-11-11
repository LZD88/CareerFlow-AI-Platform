package cn.yq.modules.knowledge.domain.vo.req;

import lombok.Data;

/**
 * 知识文档分页查询请求。
 *
 * <p>学生知识库页面和代码知识库页面复用同一张表、同一套查询接口，
 * 通过 sourceType 和 permissionScope 做页面级隔离。</p>
 */
@Data
public class KnowledgeDocumentQueryReq {

    /**
     * 页码，从 1 开始。
     */
    private Integer pageNum = 1;

    /**
     * 每页数量。
     */
    private Integer pageSize = 10;

    /**
     * 标题、文档编码或来源路径关键字。
     */
    private String keyword;

    /**
     * 文档状态：PENDING / INDEXING / INDEXED / FAILED / DISABLED。
     */
    private String status;

    /**
     * 课程 ID。仅学生知识库页面使用。
     */
    private Long courseId;

    /**
     * 学习范围：ALL / ONLINE / OFFLINE。仅学生知识库页面使用。
     */
    private String studyMode;

    /**
     * 来源类型过滤，例如 generated_java_md。
     */
    private String sourceType;

    /**
     * 权限范围或业务域过滤，例如 course / log_diagnosis。
     */
    private String permissionScope;
}
