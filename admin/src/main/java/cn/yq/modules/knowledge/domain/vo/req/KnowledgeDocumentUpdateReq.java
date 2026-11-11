package cn.yq.modules.knowledge.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台更新知识文档请求。
 *
 * <p>更新会触发重新切片和向量化：Java 先生成新 version 并入库新 chunk，
 * 成功后再删除旧 version 的 Milvus chunk。</p>
 */
@Data
public class KnowledgeDocumentUpdateReq {

    @NotBlank(message = "文档标题不能为空")
    private String title;

    @NotBlank(message = "文档内容不能为空")
    private String content;

    private Long courseId;
    private String studyMode = "ALL";
    private String sourceType = "manual";
    private String sourcePath;
    private String permissionScope = "course";
}
