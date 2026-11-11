package cn.yq.modules.mediaagent.domain.vo.res;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 保存自媒体文字草稿响应。
 */
@Data
public class MediaDraftSaveRes {

    private Long projectId;

    private Map<String, Long> draftIds = new LinkedHashMap<>();
}
