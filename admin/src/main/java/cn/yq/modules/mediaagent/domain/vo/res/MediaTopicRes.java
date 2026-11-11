package cn.yq.modules.mediaagent.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 自媒体选题生成响应。
 */
@Data
public class MediaTopicRes {

    /**
     * 原始创作主题。
     */
    private String theme = "";

    /**
     * 选题列表。
     */
    private List<MediaTopicItemRes> topics = new ArrayList<>();
}
