package cn.yq.modules.mediaagent.service;

import cn.yq.modules.mediaagent.domain.vo.req.MediaDraftSaveReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTopicReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTextDraftReq;
import cn.yq.modules.mediaagent.domain.vo.res.MediaDraftSaveRes;
import cn.yq.modules.mediaagent.domain.vo.res.MediaTextDraftRes;
import cn.yq.modules.mediaagent.domain.vo.res.MediaTopicRes;

public interface MediaAgentService {

    /**
     * 根据用户输入主题生成自媒体选题方向。
     */
    MediaTopicRes generateTopics(MediaTopicReq request);

    /**
     * 根据选中的选题方向生成平台文字草稿。
     */
    MediaTextDraftRes generateTextDraft(MediaTextDraftReq request);

    /**
     * 保存当前编辑后的文字草稿。
     */
    MediaDraftSaveRes saveDraft(MediaDraftSaveReq request, Long loginUserId);
}
