package cn.yq.modules.teaching.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.domain.entity.ClassScheduleEntity;
import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import cn.yq.modules.teaching.domain.entity.CourseEntity;
import cn.yq.modules.teaching.domain.entity.HomeworkAssignmentEntity;
import cn.yq.modules.teaching.domain.entity.HomeworkSubmissionEntity;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAnswerManageReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentAdminQueryReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentSaveReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkAssignmentStatusUpdateReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkSubmissionAdminQueryReq;
import cn.yq.modules.teaching.domain.vo.req.HomeworkSubmissionReviewReq;
import cn.yq.modules.teaching.domain.vo.res.HomeworkAssignmentAdminItemRes;
import cn.yq.modules.teaching.domain.vo.res.HomeworkPublishContextRes;
import cn.yq.modules.teaching.domain.vo.res.HomeworkSubmissionAdminItemRes;
import cn.yq.modules.teaching.mapper.ClassMapper;
import cn.yq.modules.teaching.mapper.ClassScheduleMapper;
import cn.yq.modules.teaching.mapper.CourseDetailMapper;
import cn.yq.modules.teaching.mapper.CourseMapper;
import cn.yq.modules.teaching.mapper.HomeworkAssignmentMapper;
import cn.yq.modules.teaching.mapper.HomeworkSubmissionMapper;
import cn.yq.modules.teaching.service.HomeworkAssignmentService;
import cn.yq.modules.upload.service.TosService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeworkAssignmentServiceImpl implements HomeworkAssignmentService {

    private static final long DOWNLOAD_PRESIGN_EXPIRE_SECONDS = 60 * 60L;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final HomeworkAssignmentMapper homeworkAssignmentMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final CourseDetailMapper courseDetailMapper;
    private final ClassScheduleMapper classScheduleMapper;
    private final TosService tosService;

    @Override
    public PageResult<HomeworkAssignmentAdminItemRes> page(HomeworkAssignmentAdminQueryReq request) {
        Page<HomeworkAssignmentEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<HomeworkAssignmentAdminItemRes> records = homeworkAssignmentMapper.selectAdminPage(
                        normalize(request.getTitle()),
                        request.getClassId(),
                        normalize(request.getStatus())
                ).stream()
                .map(this::toRes)
                .toList();
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public HomeworkAssignmentAdminItemRes detail(Long id) {
        return toRes(requireAssignment(id));
    }

    @Override
    public HomeworkPublishContextRes publishContext(Long classId, Date homeworkDate) {
        if (classId == null) {
            throw new BusinessException("班级不能为空");
        }
        if (homeworkDate == null) {
            throw new BusinessException("作业日期不能为空");
        }
        ClassEntity classInfo = requireClass(classId);
        HomeworkPublishContextRes res = new HomeworkPublishContextRes();
        res.setClassId(classInfo.getId());
        res.setClassTerm(classInfo.getClassTerm());
        res.setCampusName(classInfo.getCampusName());
        res.setCourseId(classInfo.getCourseId());
        res.setCourseName(classInfo.getCourseName());
        res.setHomeworkDate(homeworkDate);

        ClassScheduleEntity schedule = classScheduleMapper.selectByClassIdAndDateRange(classId, homeworkDate, homeworkDate)
                .stream()
                .findFirst()
                .orElse(null);
        if (schedule != null) {
            res.setCourseId(schedule.getCourseId() == null ? classInfo.getCourseId() : schedule.getCourseId());
            res.setCourseDetailId(schedule.getCourseDetailId());
            res.setCourseDayNum(schedule.getCourseDayNum());
            res.setCourseContentSnapshot(schedule.getCourseContent());
            res.setScheduleType(schedule.getScheduleType());
            return fillCourseName(res);
        }

        CourseDetailEntity courseDetail = resolveCourseDetailByDate(classInfo, homeworkDate);
        if (courseDetail != null) {
            res.setCourseDetailId(courseDetail.getId());
            res.setCourseDayNum(courseDetail.getDayNum());
            res.setCourseContentSnapshot(courseDetail.getCourseContent());
            res.setScheduleType("CLASS");
        }
        return fillCourseName(res);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(HomeworkAssignmentSaveReq request, Long publishTeacherId) {
        ValidationContext context = validateSaveRequest(request);
        HomeworkAssignmentEntity entity = new HomeworkAssignmentEntity();
        applyEditableFields(entity, request);
        entity.setPublishTeacherId(publishTeacherId);
        entity.setPublishDate(new Date());
        entity.setStatus("PUBLISHED");
        entity.setAnswerPublishStatus("UNPUBLISHED");
        entity.setStudentCountSnapshot(context.classInfo.getStudentCount());
        entity.setDeleted(false);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        homeworkAssignmentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, HomeworkAssignmentSaveReq request) {
        HomeworkAssignmentEntity entity = requireAssignment(id);
        ValidationContext context = validateSaveRequest(request);
        applyEditableFields(entity, request);
        entity.setStudentCountSnapshot(context.classInfo.getStudentCount());
        entity.setUpdateTime(new Date());
        homeworkAssignmentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, HomeworkAssignmentStatusUpdateReq request, Long operatorId) {
        HomeworkAssignmentEntity entity = requireAssignment(id);
        String status = normalize(request.getStatus());
        if (!"PUBLISHED".equals(status) && !"CLOSED".equals(status)) {
            throw new BusinessException("不支持的作业状态");
        }
        entity.setStatus(status);
        if ("CLOSED".equals(status)) {
            entity.setCloseTime(new Date());
            entity.setCloseTeacherId(operatorId);
        } else {
            entity.setCloseTime(null);
            entity.setCloseTeacherId(null);
        }
        entity.setUpdateTime(new Date());
        homeworkAssignmentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manageAnswer(Long id, HomeworkAnswerManageReq request, Long operatorId) {
        HomeworkAssignmentEntity entity = requireAssignment(id);
        entity.setAnswerAttachmentName(normalize(request.getAnswerAttachmentName()));
        entity.setAnswerAttachmentUrl(normalize(request.getAnswerAttachmentUrl()));
        entity.setAnswerAttachmentType(normalize(request.getAnswerAttachmentType()));
        if (Boolean.TRUE.equals(request.getPublishAnswer())) {
            if (StrUtil.isBlank(entity.getAnswerAttachmentUrl())) {
                throw new BusinessException("请先上传答案附件再发布");
            }
            entity.setAnswerPublishStatus("PUBLISHED");
            entity.setAnswerPublishTime(new Date());
            entity.setAnswerPublishTeacherId(operatorId);
        } else {
            entity.setAnswerPublishStatus("UNPUBLISHED");
            entity.setAnswerPublishTime(null);
            entity.setAnswerPublishTeacherId(null);
        }
        entity.setUpdateTime(new Date());
        homeworkAssignmentMapper.updateById(entity);
    }

    @Override
    public PageResult<HomeworkSubmissionAdminItemRes> submissionPage(Long id, HomeworkSubmissionAdminQueryReq request) {
        requireAssignment(id);
        Page<HomeworkSubmissionAdminItemRes> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<HomeworkSubmissionAdminItemRes> records = homeworkSubmissionMapper.selectAdminPage(
                        id,
                        normalize(request.getStudentCode()),
                        normalize(request.getStudentName()),
                        normalize(request.getStatus())
                ).stream()
                .map(this::toSubmissionRes)
                .toList();
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewSubmission(Long id, Long submissionId, HomeworkSubmissionReviewReq request, Long operatorId) {
        requireAssignment(id);
        HomeworkSubmissionEntity entity = homeworkSubmissionMapper.selectById(submissionId);
        if (entity == null || !id.equals(entity.getAssignmentId())) {
            throw new BusinessException("提交记录不存在");
        }
        String status = normalize(request.getStatus());
        if (!"REVIEWED".equals(status) && !"RESUBMIT_REQUIRED".equals(status)) {
            throw new BusinessException("仅支持标记为已批改或待重交");
        }
        if ("REVIEWED".equals(status) && request.getScore() == null) {
            throw new BusinessException("请填写得分");
        }
        entity.setStatus(status);
        entity.setScore(request.getScore());
        entity.setTeacherComment(normalize(request.getTeacherComment()));
        entity.setReviewTeacherId(operatorId);
        entity.setReviewTime(new Date());
        entity.setUpdateTime(new Date());
        homeworkSubmissionMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireAssignment(id);
        homeworkAssignmentMapper.softDeleteById(id);
    }

    private HomeworkSubmissionAdminItemRes toSubmissionRes(HomeworkSubmissionAdminItemRes item) {
        item.setAttachmentDownloadUrl(signDownloadUrl(item.getAttachmentUrl()));
        return item;
    }

    private HomeworkAssignmentAdminItemRes toRes(HomeworkAssignmentEntity entity) {
        HomeworkAssignmentAdminItemRes res = new HomeworkAssignmentAdminItemRes();
        res.setId(entity.getId());
        res.setTitle(entity.getTitle());
        res.setDescription(entity.getDescription());
        res.setClassId(entity.getClassId());
        res.setClassTerm(entity.getClassTerm());
        res.setCampusName(entity.getCampusName());
        res.setCourseId(entity.getCourseId());
        res.setCourseName(entity.getCourseName());
        res.setCourseDetailId(entity.getCourseDetailId());
        res.setCourseDayNum(entity.getCourseDayNum());
        res.setCourseContentSnapshot(entity.getCourseContentSnapshot());
        res.setHomeworkDate(entity.getHomeworkDate());
        res.setScheduleType(entity.getScheduleType());
        res.setAttachmentName(entity.getAttachmentName());
        res.setAttachmentUrl(entity.getAttachmentUrl());
        res.setAttachmentDownloadUrl(signDownloadUrl(entity.getAttachmentUrl()));
        res.setAttachmentType(entity.getAttachmentType());
        res.setAnswerAttachmentName(entity.getAnswerAttachmentName());
        res.setAnswerAttachmentUrl(entity.getAnswerAttachmentUrl());
        res.setAnswerAttachmentDownloadUrl(signDownloadUrl(entity.getAnswerAttachmentUrl()));
        res.setAnswerAttachmentType(entity.getAnswerAttachmentType());
        res.setAnswerPublishStatus(entity.getAnswerPublishStatus());
        res.setAnswerPublishTime(entity.getAnswerPublishTime());
        res.setAnswerPublishTeacherId(entity.getAnswerPublishTeacherId());
        res.setAnswerPublishTeacherName(entity.getAnswerPublishTeacherName());
        res.setPublishTeacherId(entity.getPublishTeacherId());
        res.setPublishTeacherName(entity.getPublishTeacherName());
        res.setPublishDate(entity.getPublishDate());
        res.setDeadline(entity.getDeadline());
        res.setStatus(entity.getStatus());
        res.setAllowResubmit(Boolean.TRUE.equals(entity.getAllowResubmit()));
        res.setStudentCountSnapshot(entity.getStudentCountSnapshot() == null ? 0 : entity.getStudentCountSnapshot());
        res.setSubmittedCount(entity.getSubmittedCount() == null ? 0 : entity.getSubmittedCount());
        return res;
    }

    private HomeworkPublishContextRes fillCourseName(HomeworkPublishContextRes res) {
        if (res.getCourseId() != null) {
            CourseEntity course = courseMapper.selectById(res.getCourseId());
            if (course != null) {
                res.setCourseName(course.getCourseName());
            }
        }
        return res;
    }

    private ValidationContext validateSaveRequest(HomeworkAssignmentSaveReq request) {
        ClassEntity classInfo = requireClass(request.getClassId());
        if (!classInfo.getCourseId().equals(request.getCourseId())) {
            throw new BusinessException("作业课程与班级当前课程不一致");
        }
        requireCourse(request.getCourseId());
        if (request.getCourseDetailId() != null) {
            CourseDetailEntity detail = courseDetailMapper.selectById(request.getCourseDetailId());
            if (detail == null || !request.getCourseId().equals(detail.getCourseId())) {
                throw new BusinessException("课程明细不存在或不属于当前课程");
            }
        }
        if (request.getDeadline() != null && request.getDeadline().before(toDateAtStartOfDay(request.getHomeworkDate()))) {
            throw new BusinessException("截止时间不能早于作业日期");
        }
        return new ValidationContext(classInfo);
    }

    private void applyEditableFields(HomeworkAssignmentEntity entity, HomeworkAssignmentSaveReq request) {
        entity.setClassId(request.getClassId());
        entity.setHomeworkDate(request.getHomeworkDate());
        entity.setCourseId(request.getCourseId());
        entity.setCourseDetailId(request.getCourseDetailId());
        entity.setCourseContentSnapshot(normalize(request.getCourseContentSnapshot()));
        entity.setTitle(normalize(request.getTitle()));
        entity.setDescription(normalize(request.getDescription()));
        entity.setAttachmentName(normalize(request.getAttachmentName()));
        entity.setAttachmentUrl(normalize(request.getAttachmentUrl()));
        entity.setAttachmentType(normalize(request.getAttachmentType()));
        entity.setDeadline(request.getDeadline());
        entity.setAllowResubmit(Boolean.TRUE.equals(request.getAllowResubmit()));
    }

    private CourseDetailEntity resolveCourseDetailByDate(ClassEntity classInfo, Date homeworkDate) {
        if (classInfo.getStartTime() == null || homeworkDate == null) {
            return null;
        }
        LocalDate classStart = toLocalDate(classInfo.getStartTime());
        LocalDate targetDate = toLocalDate(homeworkDate);
        long dayOffset = ChronoUnit.DAYS.between(classStart, targetDate) + 1;
        if (dayOffset <= 0) {
            return null;
        }
        return courseDetailMapper.selectByCourseId(classInfo.getCourseId()).stream()
                .filter(item -> item.getDayNum() != null && item.getDayNum() == dayOffset)
                .findFirst()
                .orElse(null);
    }

    private String signDownloadUrl(String rawUrl) {
        String objectKey = resolveObjectKey(rawUrl);
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
            String path = URI.create(trimmed).getPath();
            return StrUtil.isBlank(path) ? null : StrUtil.removePrefix(path, "/");
        } catch (Exception ex) {
            return null;
        }
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    private ClassEntity requireClass(Long classId) {
        ClassEntity classInfo = classMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException("班级不存在");
        }
        return classInfo;
    }

    private HomeworkAssignmentEntity requireAssignment(Long id) {
        HomeworkAssignmentEntity entity = homeworkAssignmentMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException("作业不存在");
        }
        return entity;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZONE_ID).toLocalDate();
    }

    private Date toDateAtStartOfDay(Date date) {
        return Date.from(toLocalDate(date).atStartOfDay(ZONE_ID).toInstant());
    }

    private record ValidationContext(ClassEntity classInfo) {
    }
}
