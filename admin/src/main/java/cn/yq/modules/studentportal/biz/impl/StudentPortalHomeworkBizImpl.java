package cn.yq.modules.studentportal.biz.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.biz.StudentPortalHomeworkBiz;
import cn.yq.modules.studentportal.context.StudentPortalAccessContextHolder;
import cn.yq.modules.studentportal.domain.StudentPortalAccessContext;
import cn.yq.modules.studentportal.domain.vo.req.StudentHomeworkSubmitReq;
import cn.yq.modules.studentportal.domain.vo.req.StudentHomeworkUploadPresignReq;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkDetailRes;
import cn.yq.modules.studentportal.domain.vo.res.StudentHomeworkItemRes;
import cn.yq.modules.teaching.domain.entity.HomeworkAssignmentEntity;
import cn.yq.modules.teaching.domain.entity.HomeworkSubmissionEntity;
import cn.yq.modules.teaching.mapper.HomeworkAssignmentMapper;
import cn.yq.modules.teaching.mapper.HomeworkSubmissionMapper;
import cn.yq.modules.upload.domains.vo.res.UploadPresignRes;
import cn.yq.modules.upload.service.TosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentPortalHomeworkBizImpl implements StudentPortalHomeworkBiz {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long DOWNLOAD_PRESIGN_EXPIRE_SECONDS = 60 * 60L;
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_REVIEWED = "REVIEWED";
    private static final String STATUS_RESUBMIT_REQUIRED = "RESUBMIT_REQUIRED";

    private final StudentService studentService;
    private final HomeworkAssignmentMapper homeworkAssignmentMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final TosService tosService;

    @Override
    public List<StudentHomeworkItemRes> list() {
        StudentEntity student = requireCurrentStudent();
        return homeworkAssignmentMapper.selectStudentList(student.getClassId(), student.getStudentCode()).stream()
                .map(this::signItemFiles)
                .toList();
    }

    @Override
    public StudentHomeworkDetailRes detail(Long assignmentId) {
        StudentEntity student = requireCurrentStudent();
        HomeworkAssignmentEntity assignment = requireAssignmentForStudent(assignmentId, student);
        StudentHomeworkDetailRes detail = homeworkAssignmentMapper.selectStudentDetail(assignmentId, student.getStudentCode());
        if (detail == null) {
            throw new BusinessException(404, "作业不存在");
        }
        return signDetailFiles(detail);
    }

    @Override
    public UploadPresignRes createUploadPresign(StudentHomeworkUploadPresignReq request) {
        StudentEntity student = requireCurrentStudent();
        String fileName = StrUtil.trim(request.getFileName());
        if (StrUtil.isBlank(fileName) || !StrUtil.endWithIgnoreCase(fileName, ".md")) {
            throw new BusinessException("当前仅支持上传 .md 文档");
        }
        String objectName = buildObjectName(student.getStudentCode(), fileName);
        String contentType = StrUtil.isBlank(request.getContentType()) ? "text/markdown" : request.getContentType().trim();
        return tosService.createUploadPresign(objectName, contentType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long assignmentId, StudentHomeworkSubmitReq request) {
        StudentEntity student = requireCurrentStudent();
        HomeworkAssignmentEntity assignment = requireAssignmentForStudent(assignmentId, student);
        validateSubmissionRequest(assignment, request);

        HomeworkSubmissionEntity existing = homeworkSubmissionMapper.selectByAssignmentIdAndStudentNo(assignmentId, student.getStudentCode());
        Date now = new Date();
        if (existing == null) {
            HomeworkSubmissionEntity entity = new HomeworkSubmissionEntity();
            entity.setAssignmentId(assignmentId);
            entity.setStudentNo(student.getStudentCode());
            entity.setStudentNameSnapshot(student.getStudentName());
            fillSubmission(entity, request, now);
            entity.setStatus(STATUS_SUBMITTED);
            entity.setVersion(1);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            homeworkSubmissionMapper.insert(entity);
            return entity.getId();
        }

        if (!canResubmit(assignment, existing)) {
            throw new BusinessException("当前作业不允许重复提交");
        }
        existing.setStudentNameSnapshot(student.getStudentName());
        fillSubmission(existing, request, now);
        existing.setStatus(STATUS_SUBMITTED);
        existing.setScore(null);
        existing.setTeacherComment(null);
        existing.setReviewTeacherId(null);
        existing.setReviewTime(null);
        existing.setVersion(existing.getVersion() == null ? 1 : existing.getVersion() + 1);
        existing.setUpdateTime(now);
        homeworkSubmissionMapper.updateById(existing);
        return existing.getId();
    }

    private StudentEntity requireCurrentStudent() {
        StudentPortalAccessContext accessContext = StudentPortalAccessContextHolder.get();
        if (accessContext == null || StrUtil.isBlank(accessContext.getStudentCode())) {
            throw new BusinessException(401, "未登录或登录状态失效");
        }
        StudentEntity student = studentService.findByStudentCode(accessContext.getStudentCode());
        if (student == null) {
            throw new BusinessException(404, "学员信息不存在");
        }
        if (student.getClassId() == null) {
            throw new BusinessException("当前学员还未分配班级，暂时无法查看作业");
        }
        return student;
    }

    private HomeworkAssignmentEntity requireAssignmentForStudent(Long assignmentId, StudentEntity student) {
        HomeworkAssignmentEntity assignment = homeworkAssignmentMapper.selectById(assignmentId);
        if (assignment == null || !student.getClassId().equals(assignment.getClassId())) {
            throw new BusinessException(404, "作业不存在");
        }
        return assignment;
    }

    private void validateSubmissionRequest(HomeworkAssignmentEntity assignment, StudentHomeworkSubmitReq request) {
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new BusinessException("当前作业已关闭，暂时不能提交");
        }
        if (assignment.getDeadline() != null && assignment.getDeadline().before(new Date())) {
            throw new BusinessException("当前作业已过截止时间");
        }
        String content = normalize(request.getSubmitContent());
        String attachmentUrl = normalize(request.getAttachmentUrl());
        if (StrUtil.isBlank(content) && StrUtil.isBlank(attachmentUrl)) {
            throw new BusinessException("提交说明和附件不能同时为空");
        }
        String attachmentName = normalize(request.getAttachmentName());
        if (StrUtil.isNotBlank(attachmentName) && !StrUtil.endWithIgnoreCase(attachmentName, ".md")) {
            throw new BusinessException("当前仅支持上传 .md 文档");
        }
    }

    private boolean canResubmit(HomeworkAssignmentEntity assignment, HomeworkSubmissionEntity existing) {
        if (STATUS_RESUBMIT_REQUIRED.equals(existing.getStatus())) {
            return true;
        }
        return Boolean.TRUE.equals(assignment.getAllowResubmit());
    }

    private void fillSubmission(HomeworkSubmissionEntity entity, StudentHomeworkSubmitReq request, Date now) {
        entity.setSubmitContent(normalize(request.getSubmitContent()));
        entity.setAttachmentName(normalize(request.getAttachmentName()));
        entity.setAttachmentUrl(normalize(request.getAttachmentUrl()));
        entity.setAttachmentType(normalize(request.getAttachmentType()));
        entity.setSubmitTime(now);
    }

    private StudentHomeworkItemRes signItemFiles(StudentHomeworkItemRes item) {
        item.setAttachmentDownloadUrl(signDownloadUrl(item.getAttachmentDownloadUrl()));
        item.setAnswerAttachmentDownloadUrl(signAnswerDownloadUrl(item.getAnswerPublishStatus(), item.getAnswerAttachmentDownloadUrl()));
        item.setSubmissionAttachmentDownloadUrl(signDownloadUrl(item.getSubmissionAttachmentDownloadUrl()));
        return item;
    }

    private StudentHomeworkDetailRes signDetailFiles(StudentHomeworkDetailRes detail) {
        detail.setAttachmentDownloadUrl(signDownloadUrl(detail.getAttachmentDownloadUrl()));
        detail.setAnswerAttachmentDownloadUrl(signAnswerDownloadUrl(detail.getAnswerPublishStatus(), detail.getAnswerAttachmentDownloadUrl()));
        detail.setSubmissionAttachmentDownloadUrl(signDownloadUrl(detail.getSubmissionAttachmentDownloadUrl()));
        return detail;
    }

    private String signAnswerDownloadUrl(String publishStatus, String fileUrlOrKey) {
        if (!"PUBLISHED".equals(publishStatus)) {
            return null;
        }
        return signDownloadUrl(fileUrlOrKey);
    }

    private String signDownloadUrl(String fileUrlOrKey) {
        String objectKey = resolveObjectKey(fileUrlOrKey);
        if (StrUtil.isBlank(objectKey)) {
            return null;
        }
        return tosService.createDownloadPresign(objectKey, DOWNLOAD_PRESIGN_EXPIRE_SECONDS);
    }

    private String resolveObjectKey(String fileUrlOrKey) {
        String trimmed = StrUtil.trim(fileUrlOrKey);
        if (StrUtil.isBlank(trimmed)) {
            return null;
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return trimmed;
        }
        try {
            String path = java.net.URI.create(trimmed).getPath();
            if (StrUtil.isBlank(path)) {
                return null;
            }
            return StrUtil.removePrefix(path, "/");
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildObjectName(String studentCode, String originalFilename) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        return "student/homeworks/" + studentCode + "/" + datePath + "/" + buildStoredFileName(originalFilename);
    }

    private String buildStoredFileName(String originalFilename) {
        String ext = ".md";
        if (StrUtil.isNotBlank(originalFilename)) {
            int index = originalFilename.lastIndexOf('.');
            if (index >= 0 && index < originalFilename.length() - 1) {
                String rawExt = originalFilename.substring(index + 1).replaceAll("[^a-zA-Z0-9]", "");
                if (StrUtil.isNotBlank(rawExt)) {
                    ext = "." + rawExt.toLowerCase();
                }
            }
        }
        return IdUtil.simpleUUID() + RandomUtil.randomString("abcdefghijklmnopqrstuvwxyz0123456789", 6) + ext;
    }

    private String normalize(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }
}
