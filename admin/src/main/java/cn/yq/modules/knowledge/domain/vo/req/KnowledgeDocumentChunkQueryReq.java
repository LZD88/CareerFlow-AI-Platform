package cn.yq.modules.knowledge.domain.vo.req;

import lombok.Data;

/**
 * 后台管理页查询某个知识文档 chunk 明细的请求参数。
 *
 * <p>这里不允许前端传 documentId/version。服务端会根据路径中的文档 id 查库后确定，
 * 保证只能查看当前系统已登记文档对应的向量切片。</p>
 */
@Data
public class KnowledgeDocumentChunkQueryReq {

    /**
     * 页码，从 1 开始。
     */
    private Integer pageNum = 1;

    /**
     * 每页数量，默认 20。
     */
    private Integer pageSize = 20;

    /**
     * 是否返回完整向量。页面默认 false，只看向量维度和预览值。
     */
    private Boolean includeVector = Boolean.FALSE;
}
