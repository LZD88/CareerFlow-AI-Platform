package cn.yq.modules.mediaagent.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.mediaagent.domain.vo.req.MediaDraftSaveReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTextDraftReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTopicReq;
import cn.yq.modules.mediaagent.domain.vo.res.MediaDraftSaveRes;
import cn.yq.modules.mediaagent.domain.vo.res.MediaTextDraftRes;
import cn.yq.modules.mediaagent.domain.vo.res.MediaTopicRes;
import cn.yq.modules.mediaagent.service.MediaAgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media-agent")
public class MediaAgentController {

    private final MediaAgentService mediaAgentService;

    @PostMapping("/topics")
    public ApiResponse<MediaTopicRes> generateTopics(@RequestBody @Valid MediaTopicReq request) {
        return ApiResponse.success(mediaAgentService.generateTopics(request));
    }

    @PostMapping("/drafts/generate-text")
    public ApiResponse<MediaTextDraftRes> generateTextDraft(@RequestBody @Valid MediaTextDraftReq request) {
        return ApiResponse.success(mediaAgentService.generateTextDraft(request));
    }

    @PostMapping("/drafts/save")
    public ApiResponse<MediaDraftSaveRes> saveDraft(@RequestBody @Valid MediaDraftSaveReq request,
                                                    HttpServletRequest servletRequest) {
        return ApiResponse.success("草稿保存成功", mediaAgentService.saveDraft(request, getLoginUserId(servletRequest)));
    }

    private Long getLoginUserId(HttpServletRequest request) {
        Object value = request.getAttribute("loginUserId");
        if (value == null) {
            throw new BusinessException(401, "登录状态已失效");
        }
        return Long.parseLong(value.toString());
    }
}
