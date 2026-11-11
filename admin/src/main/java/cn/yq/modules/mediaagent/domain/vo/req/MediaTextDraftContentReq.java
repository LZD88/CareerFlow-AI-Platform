package cn.yq.modules.mediaagent.domain.vo.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端提交保存的自媒体文字草稿内容。
 *
 * <p>这个类用于保存请求，避免请求对象直接依赖生成接口的 Response VO。</p>
 */
@Data
public class MediaTextDraftContentReq {

    private String theme = "";

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
