package cn.yq.modules.mediaagent.domain.vo.req;

import cn.yq.modules.mediaagent.domain.vo.res.MediaTopicItemRes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自媒体文字草稿生成请求。
 */
@Data
public class MediaTextDraftReq {

    /**
     * 用户输入的创作主题。
     */
    @NotBlank(message = "创作主题不能为空")
    @Size(max = 200, message = "创作主题不能超过200个字符")
    private String theme;

    /**
     * 用户选中的选题方向。
     */
    @Valid
    @NotNull(message = "选题不能为空")
    private MediaTopicItemRes topic;

    /**
     * 内容风格。
     */
    @Size(max = 100, message = "内容风格不能超过100个字符")
    private String style = "";
}
