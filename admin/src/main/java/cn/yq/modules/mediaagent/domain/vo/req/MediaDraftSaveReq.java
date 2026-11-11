package cn.yq.modules.mediaagent.domain.vo.req;

import cn.yq.modules.mediaagent.domain.vo.res.MediaTopicItemRes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存自媒体文字草稿请求。
 */
@Data
public class MediaDraftSaveReq {

    @NotBlank(message = "创作主题不能为空")
    @Size(max = 200, message = "创作主题不能超过200个字符")
    private String theme;

    @Valid
    @NotNull(message = "选题不能为空")
    private MediaTopicItemRes topic;

    @Size(max = 100, message = "内容风格不能超过100个字符")
    private String style = "";

    @Valid
    @NotNull(message = "文字草稿不能为空")
    private MediaTextDraftContentReq draft;
}
