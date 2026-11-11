package cn.yq.modules.interviewreview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.PythonAgentProperties;
import cn.yq.modules.interviewreview.domain.entity.InterviewQuestionBankEntity;
import cn.yq.modules.interviewreview.domain.entity.InterviewQuestionSourceEntity;
import cn.yq.modules.interviewreview.domain.entity.InterviewReviewTaskEntity;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewQuestionExtractPythonReq;
import cn.yq.modules.interviewreview.domain.vo.req.InterviewQuestionQueryReq;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionBankRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionExtractPythonRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionGenerateRes;
import cn.yq.modules.interviewreview.domain.vo.res.InterviewQuestionSourceRes;
import cn.yq.modules.interviewreview.mapper.InterviewQuestionBankMapper;
import cn.yq.modules.interviewreview.mapper.InterviewReviewTaskMapper;
import cn.yq.modules.interviewreview.service.InterviewQuestionBankService;
import cn.yq.modules.upload.service.TosService;
import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewQuestionBankServiceImpl implements InterviewQuestionBankService {

    private static final long RESUME_EXPIRE_SECONDS = 60 * 60;

    private final InterviewQuestionBankMapper questionBankMapper;
    private final InterviewReviewTaskMapper reviewTaskMapper;
    private final PythonAgentProperties pythonAgentProperties;
    private final TosService tosService;

    @Override
    public PageResult<InterviewQuestionBankRes> page(InterviewQuestionQueryReq request) {
        Page<InterviewQuestionBankEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<InterviewQuestionBankEntity> records = questionBankMapper.selectPage(trimToNull(request.getKeyword()), trimToNull(request.getCategory()));
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records.stream().map(this::toRes).toList());
    }

    @Override
    public InterviewQuestionBankRes detail(Long id) {
        InterviewQuestionBankEntity entity = questionBankMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("面试问题不存在");
        }
        return toRes(entity);
    }

    @Override
    public List<InterviewQuestionSourceRes> sources(Long id) {
        InterviewQuestionBankEntity entity = questionBankMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("面试问题不存在");
        }
        return questionBankMapper.selectSourcesByQuestionId(id);
    }

    @Override
    public InterviewQuestionGenerateRes generateFromTask(Long taskId) {
        InterviewReviewTaskEntity task = reviewTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("面试复盘任务不存在");
        }
        if ("PROCESSING".equals(task.getQuestionStatus())) {
            throw new BusinessException("题库正在生成中，请勿重复提交");
        }
        if (StrUtil.isBlank(task.getTranscriptText())) {
            throw new BusinessException("请先完成转写，再生成问题总结");
        }
        reviewTaskMapper.updateQuestionGenerating(taskId);
        CompletableFuture.runAsync(() -> runGenerateTask(task));
        InterviewQuestionGenerateRes result = new InterviewQuestionGenerateRes();
        result.setExtractedCount(0);
        result.setCreatedCount(0);
        result.setMergedCount(0);
        result.setStatus("PROCESSING");
        return result;
    }

    private void runGenerateTask(InterviewReviewTaskEntity task) {
        Long taskId = task.getId();
        try {
            InterviewQuestionGenerateRes result = extractAndSave(task);
            reviewTaskMapper.updateQuestionGenerated(
                    taskId,
                    result.getExtractedCount(),
                    result.getCreatedCount(),
                    result.getMergedCount()
            );
            log.info("面试题库生成完成 taskId={}, extracted={}, created={}, merged={}",
                    taskId, result.getExtractedCount(), result.getCreatedCount(), result.getMergedCount());
        } catch (BusinessException e) {
            log.error("面试题库生成失败 taskId={}", taskId, e);
            reviewTaskMapper.updateQuestionFailed(taskId, e.getMessage());
        } catch (Exception e) {
            log.error("面试题库生成失败 taskId={}", taskId, e);
            reviewTaskMapper.updateQuestionFailed(taskId, "面试题库生成失败：" + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected InterviewQuestionGenerateRes extractAndSave(InterviewReviewTaskEntity task) {
        Long taskId = task.getId();
        InterviewQuestionExtractPythonRes response = callPythonExtract(task);
        List<InterviewQuestionExtractPythonRes.Question> questions = response.getQuestions() == null ? List.of() : response.getQuestions();
        int created = 0;
        int merged = 0;
        for (InterviewQuestionExtractPythonRes.Question question : questions) {
            if (StrUtil.isBlank(question.getNormalizedQuestion()) || StrUtil.isBlank(question.getStandardAnswer())) {
                continue;
            }
            String normalized = normalizeQuestion(question.getNormalizedQuestion());
            InterviewQuestionBankEntity existed = questionBankMapper.selectByNormalizedQuestion(normalized);
            Long questionId;
            if (existed == null) {
                InterviewQuestionBankEntity entity = new InterviewQuestionBankEntity();
                entity.setNormalizedQuestion(normalized);
                entity.setCategory(trimToNull(question.getCategory()));
                entity.setTags(JSON.toJSONString(nullToEmpty(question.getTags())));
                entity.setStandardAnswer(question.getStandardAnswer().trim());
                entity.setShortAnswer(trimToNull(question.getShortAnswer()));
                entity.setFollowUpQuestions(JSON.toJSONString(nullToEmpty(question.getFollowUpQuestions())));
                entity.setPitfalls(JSON.toJSONString(nullToEmpty(question.getPitfalls())));
                entity.setSourceCount(1);
                entity.setLastSourceTaskId(taskId);
                questionBankMapper.insertQuestion(entity);
                questionId = entity.getId();
                created++;
            } else {
                questionId = existed.getId();
            }
            InterviewQuestionSourceEntity source = new InterviewQuestionSourceEntity();
            source.setQuestionId(questionId);
            source.setTaskId(taskId);
            source.setOriginalQuestion(StrUtil.blankToDefault(question.getOriginalQuestion(), normalized));
            source.setAnswerContext(trimToNull(question.getAnswerContext()));
            source.setStudentAnswerQuality(normalizeQuality(question.getStudentAnswerQuality()));
            source.setStudentAnswerScore(normalizeScore(question.getStudentAnswerScore()));
            source.setStudentAnswerAnalysis(trimToNull(question.getStudentAnswerAnalysis()));
            source.setImprovementSuggestion(trimToNull(question.getImprovementSuggestion()));
            int sourceAffected = questionBankMapper.insertSource(source);
            if (existed != null && sourceAffected == 1) {
                questionBankMapper.incrementSourceCount(questionId, taskId);
                merged++;
            }
        }
        InterviewQuestionGenerateRes result = new InterviewQuestionGenerateRes();
        result.setExtractedCount(questions.size());
        result.setCreatedCount(created);
        result.setMergedCount(merged);
        result.setStatus("DONE");
        return result;
    }

    private InterviewQuestionExtractPythonRes callPythonExtract(InterviewReviewTaskEntity task) {
        validatePythonConfig();
        InterviewQuestionExtractPythonReq request = new InterviewQuestionExtractPythonReq();
        request.setTaskId(task.getId());
        request.setTitle(task.getTitle());
        request.setInterviewRole(task.getInterviewRole());
        request.setTranscriptText(task.getTranscriptText());
        request.setResumeFilename(task.getResumeFilename());
        request.setResumeContentType(task.getResumeContentType());
        if (StrUtil.isNotBlank(task.getResumeObjectKey())) {
            request.setResumeUrl(tosService.createDownloadPresign(task.getResumeObjectKey(), RESUME_EXPIRE_SECONDS));
        }
        String url = buildPythonExtractUrl();
        int timeout = resolvePythonTimeout(pythonAgentProperties.getInterviewQuestionExtractTimeoutMs());
        log.info("调用 Python 面试题提取接口开始 taskId={}, url={}, timeoutMs={}", task.getId(), url, timeout);
        long start = System.currentTimeMillis();
        try (HttpResponse response = HttpRequest.post(url)
                .timeout(timeout)
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            long cost = System.currentTimeMillis() - start;
            log.info("调用 Python 面试题提取接口结束 taskId={}, httpStatus={}, costMs={}", task.getId(), response.getStatus(), cost);
            if (!response.isOk()) {
                throw new BusinessException("Python 面试题提取接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            InterviewQuestionExtractPythonRes result = JSON.parseObject(response.body(), InterviewQuestionExtractPythonRes.class);
            if (result == null) {
                throw new BusinessException("Python 面试题提取结果为空");
            }
            return result;
        } catch (HttpException ex) {
            log.error("Python 面试题提取接口调用异常 taskId={}, url={}", task.getId(), url, ex);
            throw new BusinessException("Python 面试题提取超时，请稍后重试");
        }
    }

    private String buildPythonExtractUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getInterviewQuestionExtractPath())
                ? pythonAgentProperties.getInterviewQuestionExtractPath().trim()
                : "/api/interview-review/extract-questions";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
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
        return pythonAgentProperties.getTimeoutMs() != null ? pythonAgentProperties.getTimeoutMs() : 300000;
    }

    private String normalizeQuestion(String question) {
        return question.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeQuality(String quality) {
        String value = trimToNull(quality);
        if (value == null) {
            return null;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "GOOD", "PARTIAL", "WRONG", "INSUFFICIENT", "UNKNOWN" -> upper;
            default -> "UNKNOWN";
        };
    }

    private Integer normalizeScore(Integer score) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(100, score));
    }

    private InterviewQuestionBankRes toRes(InterviewQuestionBankEntity entity) {
        InterviewQuestionBankRes res = new InterviewQuestionBankRes();
        res.setId(entity.getId());
        res.setNormalizedQuestion(entity.getNormalizedQuestion());
        res.setCategory(entity.getCategory());
        res.setTags(parseStringList(entity.getTags()));
        res.setStandardAnswer(entity.getStandardAnswer());
        res.setShortAnswer(entity.getShortAnswer());
        res.setFollowUpQuestions(parseStringList(entity.getFollowUpQuestions()));
        res.setPitfalls(parseStringList(entity.getPitfalls()));
        res.setSourceCount(entity.getSourceCount());
        res.setLastSourceTaskId(entity.getLastSourceTaskId());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        return res;
    }

    private List<String> parseStringList(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        try {
            return JSON.parseArray(value, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> nullToEmpty(List<String> value) {
        return value == null ? new ArrayList<>() : value;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
