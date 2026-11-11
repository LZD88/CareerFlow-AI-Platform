package cn.yq.modules.mediaagent.domain.vo.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 自媒体选题生成请求。
 */
@Data
public class MediaTopicReq {

    /**
     * 用户输入的创作主题。
     */
    @NotBlank(message = "创作主题不能为空")
    @Size(max = 200, message = "创作主题不能超过200个字符")
    private String theme;

    /**
     * 目标平台，例如 xiaohongshu、douyin。
     */
    private List<String> platforms = List.of("xiaohongshu", "douyin");

    /**
     * 目标人群。
     */
    @Size(max = 200, message = "目标人群不能超过200个字符")
    private String audience = "";

    /**
     * 内容目标，例如招生转化、品牌曝光、科普。
     */
    @Size(max = 100, message = "内容目标不能超过100个字符")
    private String goal = "";

    /**
     * 内容风格，例如专业、轻松、故事、干货。
     */
    @Size(max = 100, message = "内容风格不能超过100个字符")
    private String style = "";

    /**
     * 希望返回的选题数量。
     */
    @Min(value = 3, message = "选题数量不能小于3")
    @Max(value = 8, message = "选题数量不能大于8")
    private Integer topicCount = 6;
}
