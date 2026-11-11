package cn.yq.modules.interviewreview.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.PythonAgentProperties;
import cn.yq.modules.interviewreview.domain.entity.InterviewReviewTaskEntity;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewAudioConfirmReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewAudioPresignReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewResumeConfirmReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewReviewGeneratePythonReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewReviewTaskQueryReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewTranscribePythonReq;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewReviewGeneratePythonRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewReviewTaskRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewTranscribePythonRes;
import cn.yq.modules.interviewreview.mapper.InterviewReviewTaskMapper;
import cn.yq.modules.interviewreview.service.InterviewReviewTaskService;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import cn.yq.modules.upload.service.TosService;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewReviewTaskServiceImpl implements InterviewReviewTaskService {

    private static final long MAX_AUDIO_SIZE = 500L * 1024 * 1024;
    private static final long MAX_RESUME_SIZE = 20L * 1024 * 1024;
    private static final long AUDIO_PREVIEW_EXPIRE_SECONDS = 30 * 60;
    private static final long AUDIO_TRANSCRIBE_EXPIRE_SECONDS = 60 * 60;
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_TRANSCRIBING = "TRANSCRIBING";
    private static final String STATUS_TRANSCRIBED = "TRANSCRIBED";
    private static final String STATUS_REVIEWING = "REVIEWING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_FAILED = "FAILED";

    private final InterviewReviewTaskMapper interviewReviewTaskMapper;
    private final TosService tosService;
    private final PythonAgentProperties pythonAgentProperties;

    @Override
    public PageResult<InterviewReviewTaskRes> page(InterviewReviewTaskQueryReq request) {
        Page<InterviewReviewTaskEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<InterviewReviewTaskEntity> records = interviewReviewTaskMapper.selectPage(
                trimToNull(request.getKeyword()),
                normalizeStatus(request.getStatus())
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records.stream().map(this::toListRes).toList());
    }

    @Override
    public InterviewReviewTaskRes detail(Long id) {
        InterviewReviewTaskEntity entity = interviewReviewTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("面试复盘任务不存在");
        }
        return toRes(entity, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewReviewTaskRes uploadAudio(MultipartFile file,
                                              String title,
                                              String candidateName,
                                              String interviewRole,
                                              Long loginUserId) {
        validateAudio(file);
        String originalFilename = resolveOriginalFilename(file);
        String objectKey = buildObjectKey(originalFilename, "audio");
        String audioUrl;
        try (InputStream inputStream = file.getInputStream()) {
            audioUrl = tosService.uploadFile(objectKey, inputStream);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("面试音频上传失败 originalFilename={}", originalFilename, e);
            throw new BusinessException("面试音频上传失败：" + e.getMessage());
        }

        Date now = new Date();
        InterviewReviewTaskEntity entity = new InterviewReviewTaskEntity();
        entity.setTitle(resolveTitle(title, originalFilename));
        entity.setCandidateName(trimToNull(candidateName));
        entity.setInterviewRole(trimToNull(interviewRole));
        entity.setOriginalFilename(originalFilename);
        entity.setAudioObjectKey(objectKey);
        entity.setAudioUrl(audioUrl);
        entity.setAudioContentType(trimToNull(file.getContentType()));
        entity.setAudioSize(file.getSize());
        entity.setStatus(STATUS_UPLOADED);
        entity.setQuestionStatus("NONE");
        entity.setQuestionExtractedCount(0);
        entity.setQuestionCreatedCount(0);
        entity.setQuestionMergedCount(0);
        entity.setCreatedBy(loginUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        interviewReviewTaskMapper.insert(entity);
        return toRes(entity, true);
    }

    @Override
    public UploadPresignRes createAudioPresign(InterviewAudioPresignReq request) {
        boolean resume = "RESUME".equalsIgnoreCase(request.getFileCategory());
        if (resume) {
            validateResumeMeta(request.getFileName(), request.getContentType(), request.getFileSize());
            return tosService.createUploadPresign(buildObjectKey(request.getFileName(), "resume"), request.getContentType());
        }
        validateAudioMeta(request.getFileName(), request.getContentType(), request.getFileSize());
        return tosService.createUploadPresign(buildObjectKey(request.getFileName(), "audio"), request.getContentType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewReviewTaskRes confirmAudioUpload(InterviewAudioConfirmReq request, Long loginUserId) {
        validateAudioMeta(request.getOriginalFilename(), request.getAudioContentType(), request.getAudioSize());
        if (!request.getAudioObjectKey().startsWith("interview-review/audio/")) {
            throw new BusinessException("音频对象 Key 不合法");
        }
        if (StrUtil.isNotBlank(request.getResumeObjectKey())) {
            validateResumeMeta(request.getResumeFilename(), request.getResumeContentType(), request.getResumeSize());
            if (!request.getResumeObjectKey().startsWith("interview-review/resume/")) {
                throw new BusinessException("简历对象 Key 不合法");
            }
        }
        Date now = new Date();
        InterviewReviewTaskEntity entity = new InterviewReviewTaskEntity();
        entity.setTitle(resolveTitle(request.getTitle(), request.getOriginalFilename()));
        entity.setCandidateName(trimToNull(request.getCandidateName()));
        entity.setInterviewRole(trimToNull(request.getInterviewRole()));
        entity.setOriginalFilename(request.getOriginalFilename().trim());
        entity.setAudioObjectKey(request.getAudioObjectKey().trim());
        entity.setAudioUrl(request.getAudioUrl().trim());
        entity.setAudioContentType(trimToNull(request.getAudioContentType()));
        entity.setAudioSize(request.getAudioSize());
        entity.setResumeFilename(trimToNull(request.getResumeFilename()));
        entity.setResumeObjectKey(trimToNull(request.getResumeObjectKey()));
        entity.setResumeUrl(trimToNull(request.getResumeUrl()));
        entity.setResumeContentType(trimToNull(request.getResumeContentType()));
        entity.setResumeSize(request.getResumeSize());
        entity.setStatus(STATUS_UPLOADED);
        entity.setQuestionStatus("NONE");
        entity.setQuestionExtractedCount(0);
        entity.setQuestionCreatedCount(0);
        entity.setQuestionMergedCount(0);
        entity.setCreatedBy(loginUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        interviewReviewTaskMapper.insert(entity);
        return toRes(entity, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewReviewTaskRes confirmResumeUpload(Long id, InterviewResumeConfirmReq request) {
        InterviewReviewTaskEntity entity = interviewReviewTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("面试复盘任务不存在");
        }
        validateResumeMeta(request.getResumeFilename(), request.getResumeContentType(), request.getResumeSize());
        if (!request.getResumeObjectKey().startsWith("interview-review/resume/")) {
            throw new BusinessException("简历对象 Key 不合法");
        }
        interviewReviewTaskMapper.updateResume(
                id,
                request.getResumeFilename().trim(),
                request.getResumeObjectKey().trim(),
                request.getResumeUrl().trim(),
                trimToNull(request.getResumeContentType()),
                request.getResumeSize()
        );
        return detail(id);
    }

    @Override
    public InterviewReviewTaskRes transcribe(Long id, Boolean speakerDiarization) {
        InterviewReviewTaskEntity entity = interviewReviewTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("面试复盘任务不存在");
        }
        if (STATUS_TRANSCRIBING.equals(entity.getStatus())) {
            throw new BusinessException("任务正在转写中，请勿重复提交");
        }
        if (!STATUS_UPLOADED.equals(entity.getStatus())
                && !STATUS_FAILED.equals(entity.getStatus())
                && !STATUS_TRANSCRIBED.equals(entity.getStatus())) {
            throw new BusinessException("当前状态不支持开始转写");
        }
        validatePythonConfig();
        interviewReviewTaskMapper.updateTranscribing(id);
        log.info("面试音频转写任务已入队 taskId={}, speakerDiarization={}, pythonUrl={}",
                id, Boolean.TRUE.equals(speakerDiarization), buildPythonTranscribeUrl());
        CompletableFuture.runAsync(() -> runTranscribeTask(entity, Boolean.TRUE.equals(speakerDiarization)));
        return detail(id);
    }

    @Override
    public InterviewReviewTaskRes generateReview(Long id) {
        InterviewReviewTaskEntity entity = interviewReviewTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("面试复盘任务不存在");
        }
        if (STATUS_REVIEWING.equals(entity.getStatus())) {
            throw new BusinessException("任务正在生成复盘中，请勿重复提交");
        }
        if (!STATUS_TRANSCRIBED.equals(entity.getStatus()) && !STATUS_DONE.equals(entity.getStatus())) {
            throw new BusinessException("请先完成音频转写，再生成面试复盘");
        }
        if (StrUtil.isBlank(entity.getTranscriptText())) {
            throw new BusinessException("转写文本为空，无法生成面试复盘");
        }
        validatePythonConfig();
        interviewReviewTaskMapper.updateReviewing(id);
        log.info("面试复盘生成任务已入队 taskId={}, pythonUrl={}", id, buildPythonReviewUrl());
        CompletableFuture.runAsync(() -> runReviewTask(entity));
        return detail(id);
    }

    private void runTranscribeTask(InterviewReviewTaskEntity entity, boolean speakerDiarization) {
        Long id = entity.getId();
        try {
            log.info("面试音频转写任务开始执行 taskId={}, speakerDiarization={}, audioObjectKey={}",
                    id, speakerDiarization, entity.getAudioObjectKey());
            InterviewTranscribePythonRes response = callPythonTranscribe(entity, speakerDiarization);
            interviewReviewTaskMapper.updateTranscribed(id, response.getText());
            log.info("面试音频转写任务完成 taskId={}, textLength={}",
                    id, response.getText() == null ? 0 : response.getText().length());
        } catch (BusinessException e) {
            log.error("面试音频转写失败 taskId={}", id, e);
            interviewReviewTaskMapper.updateStatus(id, STATUS_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("面试音频转写失败 taskId={}", id, e);
            String message = "面试音频转写失败：" + e.getMessage();
            interviewReviewTaskMapper.updateStatus(id, STATUS_FAILED, message);
        }
    }

    private void runReviewTask(InterviewReviewTaskEntity entity) {
        Long id = entity.getId();
        try {
            log.info("面试复盘生成任务开始执行 taskId={}, role={}", id, entity.getInterviewRole());
            InterviewReviewGeneratePythonRes response = callPythonGenerateReview(entity);
            interviewReviewTaskMapper.updateReviewed(id, response.getReviewReport());
            log.info("面试复盘生成任务完成 taskId={}, reportLength={}",
                    id, response.getReviewReport() == null ? 0 : response.getReviewReport().length());
        } catch (BusinessException e) {
            log.error("面试复盘生成失败 taskId={}", id, e);
            interviewReviewTaskMapper.updateStatus(id, STATUS_FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("面试复盘生成失败 taskId={}", id, e);
            String message = "面试复盘生成失败：" + e.getMessage();
            interviewReviewTaskMapper.updateStatus(id, STATUS_FAILED, message);
        }
    }

    private InterviewTranscribePythonRes callPythonTranscribe(InterviewReviewTaskEntity entity, boolean speakerDiarization) {
        InterviewTranscribePythonReq request = new InterviewTranscribePythonReq();
        request.setTaskId(entity.getId());
        request.setAudioObjectKey(entity.getAudioObjectKey());
        request.setAudioUrl(tosService.createDownloadPresign(entity.getAudioObjectKey(), AUDIO_TRANSCRIBE_EXPIRE_SECONDS));
        request.setLanguage("auto");
        request.setSpeakerDiarization(speakerDiarization);
        String url = buildPythonTranscribeUrl();
        int timeout = resolvePythonTimeout(pythonAgentProperties.getInterviewReviewTranscribeTimeoutMs());
        log.info("调用 Python 面试转写接口开始 taskId={}, url={}, timeoutMs={}, speakerDiarization={}",
                entity.getId(), url, timeout, speakerDiarization);
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(url)
                .timeout(timeout)
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            long cost = System.currentTimeMillis() - start;
            log.info("调用 Python 面试转写接口结束 taskId={}, httpStatus={}, costMs={}",
                    entity.getId(), response.getStatus(), cost);
            if (!response.isOk()) {
                throw new BusinessException("Python 面试音频转写接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            InterviewTranscribePythonRes result = JSON.parseObject(response.body(), InterviewTranscribePythonRes.class);
            if (result == null || StrUtil.isBlank(result.getText())) {
                throw new BusinessException("Python 面试音频转写结果为空");
            }
            return result;
        } catch (HttpException ex) {
            long cost = System.currentTimeMillis() - start;
            log.error("Python 面试音频转写接口调用超时或网络异常 taskId={}, url={}, costMs={}",
                    entity.getId(), url, cost, ex);
            throw new BusinessException("Python 面试音频转写超时，请稍后重试");
        }
    }

    private InterviewReviewGeneratePythonRes callPythonGenerateReview(InterviewReviewTaskEntity entity) {
        InterviewReviewGeneratePythonReq request = new InterviewReviewGeneratePythonReq();
        request.setTaskId(entity.getId());
        request.setTitle(entity.getTitle());
        request.setCandidateName(entity.getCandidateName());
        request.setInterviewRole(entity.getInterviewRole());
        request.setTranscriptText(entity.getTranscriptText());
        request.setResumeFilename(entity.getResumeFilename());
        request.setResumeObjectKey(entity.getResumeObjectKey());
        request.setResumeContentType(entity.getResumeContentType());
        if (StrUtil.isNotBlank(entity.getResumeObjectKey())) {
            request.setResumeUrl(tosService.createDownloadPresign(entity.getResumeObjectKey(), AUDIO_TRANSCRIBE_EXPIRE_SECONDS));
        }
        String url = buildPythonReviewUrl();
        int timeout = resolvePythonTimeout(pythonAgentProperties.getInterviewReviewGenerateTimeoutMs());
        log.info("调用 Python 面试复盘接口开始 taskId={}, url={}, timeoutMs={}, hasResume={}",
                entity.getId(), url, timeout, StrUtil.isNotBlank(entity.getResumeObjectKey()));
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(url)
                .timeout(timeout)
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            long cost = System.currentTimeMillis() - start;
            log.info("调用 Python 面试复盘接口结束 taskId={}, httpStatus={}, costMs={}",
                    entity.getId(), response.getStatus(), cost);
            if (!response.isOk()) {
                throw new BusinessException("Python 面试复盘接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            InterviewReviewGeneratePythonRes result = JSON.parseObject(response.body(), InterviewReviewGeneratePythonRes.class);
            if (result == null || StrUtil.isBlank(result.getReviewReport())) {
                throw new BusinessException("Python 面试复盘结果为空");
            }
            return result;
        } catch (HttpException ex) {
            long cost = System.currentTimeMillis() - start;
            log.error("Python 面试复盘接口调用超时或网络异常 taskId={}, url={}, costMs={}",
                    entity.getId(), url, cost, ex);
            throw new BusinessException("Python 面试复盘超时，请稍后重试");
        }
    }

    private void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的面试音频");
        }
        String originalFilename = resolveOriginalFilename(file);
        validateAudioMeta(originalFilename, file.getContentType(), file.getSize());
    }

    private void validateAudioMeta(String originalFilename, String rawContentType, Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException("音频文件大小不能为空");
        }
        if (fileSize > MAX_AUDIO_SIZE) {
            throw new BusinessException("音频文件不能超过 500MB");
        }
        String extension = resolveExtension(originalFilename);
        String contentType = rawContentType == null ? "" : rawContentType.toLowerCase(Locale.ROOT);
        boolean validExtension = switch (extension) {
            case "mp3", "wav", "m4a", "aac", "flac", "ogg", "opus", "webm", "mp4" -> true;
            default -> false;
        };
        boolean validContentType = contentType.isBlank()
                || contentType.startsWith("audio/")
                || "video/mp4".equals(contentType)
                || "application/octet-stream".equals(contentType);
        if (!validExtension || !validContentType) {
            throw new BusinessException("仅支持 mp3、wav、m4a、aac、flac、ogg、opus、webm、mp4 音频文件");
        }
    }

    private void validateResumeMeta(String originalFilename, String rawContentType, Long fileSize) {
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException("简历文件名不能为空");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException("简历文件大小不能为空");
        }
        if (fileSize > MAX_RESUME_SIZE) {
            throw new BusinessException("简历文件不能超过 20MB");
        }
        String extension = resolveExtension(originalFilename);
        String contentType = rawContentType == null ? "" : rawContentType.toLowerCase(Locale.ROOT);
        boolean validExtension = switch (extension) {
            case "pdf", "doc", "docx", "txt", "md" -> true;
            default -> false;
        };
        boolean validContentType = contentType.isBlank()
                || "application/pdf".equals(contentType)
                || "application/msword".equals(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                || contentType.startsWith("text/")
                || "application/octet-stream".equals(contentType);
        if (!validExtension || !validContentType) {
            throw new BusinessException("仅支持 pdf、doc、docx、txt、md 简历文件");
        }
    }

    private String buildObjectKey(String originalFilename, String category) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "interview-review/" + category + "/" + datePath + "/" + buildStoredFileName(originalFilename);
    }

    private String buildStoredFileName(String originalFilename) {
        String extension = resolveExtension(originalFilename);
        String suffix = StrUtil.isBlank(extension) ? "" : "." + extension;
        return IdUtil.simpleUUID() + RandomUtil.randomString("abcdefghijklmnopqrstuvwxyz0123456789", 6) + suffix;
    }

    private String resolveOriginalFilename(MultipartFile file) {
        String originalFilename = file == null ? null : file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            return "interview-audio";
        }
        return originalFilename.trim();
    }

    private String resolveExtension(String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return "";
        }
        int index = originalFilename.lastIndexOf('.');
        if (index < 0 || index >= originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(index + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String resolveTitle(String title, String originalFilename) {
        if (StrUtil.isNotBlank(title)) {
            return title.trim();
        }
        int index = originalFilename.lastIndexOf('.');
        return index > 0 ? originalFilename.substring(0, index) : originalFilename;
    }

    private String normalizeStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
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
        return 600000;
    }

    private String buildPythonTranscribeUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getInterviewReviewTranscribePath())
                ? pythonAgentProperties.getInterviewReviewTranscribePath().trim()
                : "/api/interview-review/transcribe";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String buildPythonReviewUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getInterviewReviewGeneratePath())
                ? pythonAgentProperties.getInterviewReviewGeneratePath().trim()
                : "/api/interview-review/generate-review";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private InterviewReviewTaskRes toRes(InterviewReviewTaskEntity entity) {
        return toRes(entity, true);
    }

    private InterviewReviewTaskRes toListRes(InterviewReviewTaskEntity entity) {
        InterviewReviewTaskRes res = toRes(entity, false);
        res.setTranscriptText(null);
        res.setReviewReport(null);
        res.setFailReason(null);
        return res;
    }

    private InterviewReviewTaskRes toRes(InterviewReviewTaskEntity entity, boolean includeAudioPreviewUrl) {
        InterviewReviewTaskRes res = new InterviewReviewTaskRes();
        res.setId(entity.getId());
        res.setTitle(entity.getTitle());
        res.setCandidateName(entity.getCandidateName());
        res.setInterviewRole(entity.getInterviewRole());
        res.setOriginalFilename(entity.getOriginalFilename());
        res.setAudioObjectKey(entity.getAudioObjectKey());
        res.setAudioUrl(entity.getAudioUrl());
        if (includeAudioPreviewUrl) {
            res.setAudioPreviewUrl(createAudioPreviewUrl(entity.getAudioObjectKey()));
        }
        res.setAudioContentType(entity.getAudioContentType());
        res.setAudioSize(entity.getAudioSize());
        res.setResumeFilename(entity.getResumeFilename());
        res.setResumeObjectKey(entity.getResumeObjectKey());
        res.setResumeUrl(entity.getResumeUrl());
        if (includeAudioPreviewUrl) {
            res.setResumePreviewUrl(createDownloadPreviewUrl(entity.getResumeObjectKey()));
        }
        res.setResumeContentType(entity.getResumeContentType());
        res.setResumeSize(entity.getResumeSize());
        res.setStatus(entity.getStatus());
        res.setTranscriptText(entity.getTranscriptText());
        res.setReviewReport(entity.getReviewReport());
        res.setFailReason(entity.getFailReason());
        res.setQuestionStatus(entity.getQuestionStatus());
        res.setQuestionExtractedCount(entity.getQuestionExtractedCount());
        res.setQuestionCreatedCount(entity.getQuestionCreatedCount());
        res.setQuestionMergedCount(entity.getQuestionMergedCount());
        res.setQuestionFailReason(entity.getQuestionFailReason());
        res.setCreatedBy(entity.getCreatedBy());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }

    private String createAudioPreviewUrl(String audioObjectKey) {
        return createDownloadPreviewUrl(audioObjectKey);
    }

    private String createDownloadPreviewUrl(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            return null;
        }
        return tosService.createDownloadPresign(objectKey, AUDIO_PREVIEW_EXPIRE_SECONDS);
    }
}
