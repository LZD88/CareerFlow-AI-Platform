package cn.yq.modules.knowledge.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class KnowledgeDocumentCreateReq {

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
