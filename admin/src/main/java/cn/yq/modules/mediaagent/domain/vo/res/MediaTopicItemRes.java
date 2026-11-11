package cn.yq.modules.mediaagent.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 自媒体选题项。
 */
@Data
public class MediaTopicItemRes {

    /**
     * 选题方向，表达内容要讲什么，不是最终标题。
     */
    private String topic = "";

    /**
     * 内容角度。
     */
    private String angle = "";

    /**
     * 该选题面向的具体用户。
     */
    private String audience = "";

    /**
     * 推荐理由。
     */
    private String reason = "";

    /**
     * 建议展开的内容要点。
     */
    private List<String> contentPoints = new ArrayList<>();

    /**
     * 适配平台。
     */
    private List<String> platformFit = new ArrayList<>();

    /**
     * 推荐分。
     */
    private Integer score = 0;
}
