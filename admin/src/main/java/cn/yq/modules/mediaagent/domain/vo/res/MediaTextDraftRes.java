package cn.yq.modules.mediaagent.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 自媒体文字草稿生成响应。
 */
@Data
public class MediaTextDraftRes {

    private String theme = "";

    private MediaTopicItemRes topic;

    private XiaohongshuNoteDraft xiaohongshuNote = new XiaohongshuNoteDraft();

    private DouyinVideoDraft douyinVideo = new DouyinVideoDraft();

    private DouyinImageTextDraft douyinImageText = new DouyinImageTextDraft();

    @Data
    public static class XiaohongshuNoteDraft {
        private List<String> titles = new ArrayList<>();
        private String coverText = "";
        private String body = "";
        private List<String> imagePagePlan = new ArrayList<>();
        private List<String> hashtags = new ArrayList<>();
    }

    @Data
    public static class DouyinVideoDraft {
        private List<String> hooks = new ArrayList<>();
        private String script = "";
        private List<String> shotList = new ArrayList<>();
        private List<String> subtitles = new ArrayList<>();
        private List<String> hashtags = new ArrayList<>();
    }

    @Data
    public static class DouyinImageTextDraft {
        private List<String> titles = new ArrayList<>();
        private String coverText = "";
        private String body = "";
        private List<String> imagePagePlan = new ArrayList<>();
        private List<String> hashtags = new ArrayList<>();
    }
}
