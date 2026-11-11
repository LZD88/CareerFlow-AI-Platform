package cn.yq.modules.mediaagent.service.impl;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.PythonAgentProperties;
import cn.yq.modules.mediaagent.domain.entity.MediaDraftEntity;
import cn.yq.modules.mediaagent.domain.entity.MediaProjectEntity;
import cn.yq.modules.mediaagent.domain.vo.req.MediaDraftSaveReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTextDraftReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTextDraftContentReq;
import cn.yq.modules.mediaagent.domain.vo.req.MediaTopicReq;
import cn.yq.modules.mediaagent.domain.vo.res.MediaDraftSaveRes;
import cn.yq.modules.mediaagent.domain.vo.res.MediaTextDraftRes;
import cn.yq.modules.mediaagent.domain.vo.res.MediaTopicRes;
import cn.yq.modules.mediaagent.mapper.MediaDraftMapper;
import cn.yq.modules.mediaagent.mapper.MediaProjectMapper;
import cn.yq.modules.mediaagent.service.MediaAgentService;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 自媒体 Agent 服务。
 *
 * <p>第一版只做选题、文字草稿生成和草稿保存，不直接访问平台账号。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAgentServiceImpl implements MediaAgentService {

    private final PythonAgentProperties pythonAgentProperties;
    private final MediaProjectMapper mediaProjectMapper;
    private final MediaDraftMapper mediaDraftMapper;

    @Override
    public MediaTopicRes generateTopics(MediaTopicReq request) {
        validatePythonConfig();
        try (HttpResponse response = HttpRequest.post(buildPythonTopicsUrl())
                .timeout(resolvePythonTimeout(pythonAgentProperties.getMediaAgentTopicsTimeoutMs()))
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python 自媒体选题接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), MediaTopicRes.class);
        } catch (HttpException ex) {
            log.error("Python 自媒体选题接口调用超时或网络异常", ex);
            throw new BusinessException("Python 自媒体选题生成超时，请稍后重试");
        }
    }

    @Override
    public MediaTextDraftRes generateTextDraft(MediaTextDraftReq request) {
        validatePythonConfig();
        try (HttpResponse response = HttpRequest.post(buildPythonTextDraftUrl())
                .timeout(resolvePythonTimeout(pythonAgentProperties.getMediaAgentTextDraftTimeoutMs()))
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python 自媒体文字草稿接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), MediaTextDraftRes.class);
        } catch (HttpException ex) {
            log.error("Python 自媒体文字草稿接口调用超时或网络异常", ex);
            throw new BusinessException("Python 自媒体文字草稿生成超时，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaDraftSaveRes saveDraft(MediaDraftSaveReq request, Long loginUserId) {
        if (request.getDraft() == null) {
            throw new BusinessException("文字草稿不能为空");
        }
        Date now = new Date();
        MediaProjectEntity project = new MediaProjectEntity();
        project.setTheme(request.getTheme());
        project.setTopicJson(JSON.toJSONString(request.getTopic()));
        project.setTopicTitle(request.getTopic() == null ? "" : request.getTopic().getTopic());
        project.setStyle(request.getStyle());
        project.setStatus("DRAFT");
        project.setCreatedBy(loginUserId);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        mediaProjectMapper.insert(project);

        MediaDraftSaveRes response = new MediaDraftSaveRes();
        response.setProjectId(project.getId());
        saveXiaohongshuDraft(project.getId(), request.getDraft().getXiaohongshuNote(), loginUserId, now, response);
        saveDouyinVideoDraft(project.getId(), request.getDraft().getDouyinVideo(), loginUserId, now, response);
        saveDouyinImageTextDraft(project.getId(), request.getDraft().getDouyinImageText(), loginUserId, now, response);
        return response;
    }

    private void saveXiaohongshuDraft(Long projectId,
                                      MediaTextDraftContentReq.XiaohongshuNoteDraft draft,
                                      Long loginUserId,
                                      Date now,
                                      MediaDraftSaveRes response) {
        if (draft == null) {
            return;
        }
        MediaDraftEntity entity = baseDraft(projectId, "xiaohongshu_note", loginUserId, now);
        entity.setTitleOptions(JSON.toJSONString(draft.getTitles()));
        entity.setCoverText(draft.getCoverText());
        entity.setBody(draft.getBody());
        entity.setImagePagePlan(JSON.toJSONString(draft.getImagePagePlan()));
        entity.setHashtags(JSON.toJSONString(draft.getHashtags()));
        mediaDraftMapper.insert(entity);
        response.getDraftIds().put(entity.getContentType(), entity.getId());
    }

    private void saveDouyinVideoDraft(Long projectId,
                                      MediaTextDraftContentReq.DouyinVideoDraft draft,
                                      Long loginUserId,
                                      Date now,
                                      MediaDraftSaveRes response) {
        if (draft == null) {
            return;
        }
        MediaDraftEntity entity = baseDraft(projectId, "douyin_video", loginUserId, now);
        entity.setTitleOptions(JSON.toJSONString(draft.getHooks()));
        entity.setBody(draft.getScript());
        entity.setShotList(JSON.toJSONString(draft.getShotList()));
        entity.setSubtitles(JSON.toJSONString(draft.getSubtitles()));
        entity.setHashtags(JSON.toJSONString(draft.getHashtags()));
        mediaDraftMapper.insert(entity);
        response.getDraftIds().put(entity.getContentType(), entity.getId());
    }

    private void saveDouyinImageTextDraft(Long projectId,
                                          MediaTextDraftContentReq.DouyinImageTextDraft draft,
                                          Long loginUserId,
                                          Date now,
                                          MediaDraftSaveRes response) {
        if (draft == null) {
            return;
        }
        MediaDraftEntity entity = baseDraft(projectId, "douyin_image_text", loginUserId, now);
        entity.setTitleOptions(JSON.toJSONString(draft.getTitles()));
        entity.setCoverText(draft.getCoverText());
        entity.setBody(draft.getBody());
        entity.setImagePagePlan(JSON.toJSONString(draft.getImagePagePlan()));
        entity.setHashtags(JSON.toJSONString(draft.getHashtags()));
        mediaDraftMapper.insert(entity);
        response.getDraftIds().put(entity.getContentType(), entity.getId());
    }

    private MediaDraftEntity baseDraft(Long projectId, String contentType, Long loginUserId, Date now) {
        MediaDraftEntity entity = new MediaDraftEntity();
        entity.setProjectId(projectId);
        entity.setContentType(contentType);
        entity.setVersion(1);
        entity.setStatus("DRAFT");
        entity.setCreatedBy(loginUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private void validatePythonConfig() {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            throw new BusinessException("Python Agent 未启用");
        }
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 地址未配置");
        }
    }

    private int resolvePythonTimeout(Integer timeoutMs) {
        if (timeoutMs != null && timeoutMs > 0) {
            return timeoutMs;
        }
        if (pythonAgentProperties.getTimeoutMs() != null && pythonAgentProperties.getTimeoutMs() > 0) {
            return pythonAgentProperties.getTimeoutMs();
        }
        return 60000;
    }

    private String buildPythonTopicsUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getMediaAgentTopicsPath())
                ? pythonAgentProperties.getMediaAgentTopicsPath().trim()
                : "/api/media-agent/topics";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String buildPythonTextDraftUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getMediaAgentTextDraftPath())
                ? pythonAgentProperties.getMediaAgentTextDraftPath().trim()
                : "/api/media-agent/drafts/generate-text";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }
}
