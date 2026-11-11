package cn.yq.modules.studentportal.service.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentBindPermissionTemplateReq;
import cn.yq.modules.student.domain.vo.res.StudentPermissionTemplateBindingRes;
import cn.yq.modules.student.domain.vo.res.StudentPermissionTemplateDetailRes;
import cn.yq.modules.student.mapper.StudentMapper;
import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import cn.yq.modules.studentportal.domain.entity.StuStudentPermissionTemplateEntity;
import cn.yq.modules.studentportal.mapper.StuPermissionTemplateMapper;
import cn.yq.modules.studentportal.mapper.StuStudentPermissionTemplateMapper;
import cn.yq.modules.studentportal.service.StudentPermissionTemplateBindingManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentPermissionTemplateBindingManageServiceImpl implements StudentPermissionTemplateBindingManageService {

    private static final int STATUS_ENABLED = 1;
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_AUTO = "AUTO";
    private static final String SOURCE_NONE = "NONE";

    private final StudentMapper studentMapper;
    private final StuPermissionTemplateMapper stuPermissionTemplateMapper;
    private final StuStudentPermissionTemplateMapper stuStudentPermissionTemplateMapper;

    @Override
    public StudentPermissionTemplateDetailRes detail(Long studentId) {
        StudentEntity student = requireStudent(studentId);
        StuStudentPermissionTemplateEntity manualBindingEntity = resolveActiveBinding(studentId);
        StuPermissionTemplateEntity autoMatchedTemplate = resolveBestMatchedTemplate(student);
        StuPermissionTemplateEntity effectiveTemplate = resolveEffectiveTemplate(student);

        StudentPermissionTemplateDetailRes res = new StudentPermissionTemplateDetailRes();
        res.setStudentId(student.getId());
        res.setStudentCode(student.getStudentCode());
        res.setStudentName(student.getStudentName());
        res.setManualBinding(toBindingRes(manualBindingEntity));
        res.setAutoMatchedTemplate(autoMatchedTemplate);
        res.setEffectiveTemplate(effectiveTemplate);
        if (manualBindingEntity != null) {
            res.setSource(SOURCE_MANUAL);
        } else if (autoMatchedTemplate != null) {
            res.setSource(SOURCE_AUTO);
        } else {
            res.setSource(SOURCE_NONE);
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(Long studentId, StudentBindPermissionTemplateReq request) {
        requireStudent(studentId);
        StuPermissionTemplateEntity template = requireActiveTemplate(request.getTemplateId());
        validateEffectiveTime(request.getEffectiveStartTime(), request.getEffectiveEndTime());

        stuStudentPermissionTemplateMapper.disableByStudentId(studentId);

        StuStudentPermissionTemplateEntity existing = stuStudentPermissionTemplateMapper
                .selectByStudentIdAndTemplateId(studentId, template.getId());
        Date now = new Date();
        if (existing != null) {
            existing.setEffectiveStartTime(request.getEffectiveStartTime());
            existing.setEffectiveEndTime(request.getEffectiveEndTime());
            existing.setStatus(normalizeStatus(request.getStatus()));
            existing.setUpdatedAt(now);
            stuStudentPermissionTemplateMapper.updateById(existing);
            return;
        }

        StuStudentPermissionTemplateEntity entity = new StuStudentPermissionTemplateEntity();
        entity.setStudentId(studentId);
        entity.setTemplateId(template.getId());
        entity.setEffectiveStartTime(request.getEffectiveStartTime());
        entity.setEffectiveEndTime(request.getEffectiveEndTime());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        stuStudentPermissionTemplateMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long studentId) {
        requireStudent(studentId);
        stuStudentPermissionTemplateMapper.disableByStudentId(studentId);
    }

    @Override
    public StuPermissionTemplateEntity resolveEffectiveTemplate(StudentEntity student) {
        if (student == null || student.getId() == null) {
            return null;
        }
        StuStudentPermissionTemplateEntity binding = resolveActiveBinding(student.getId());
        if (binding != null && binding.getTemplateId() != null) {
            StuPermissionTemplateEntity template = stuPermissionTemplateMapper.selectById(binding.getTemplateId());
            if (template != null && Objects.equals(template.getStatus(), STATUS_ENABLED)) {
                return template;
            }
        }
        return resolveBestMatchedTemplate(student);
    }

    @Override
    public void fillEffectiveTemplateSummary(StudentEntity student) {
        if (student == null || student.getId() == null) {
            return;
        }
        StuStudentPermissionTemplateEntity binding = resolveActiveBinding(student.getId());
        if (binding != null) {
            StuPermissionTemplateEntity template = stuPermissionTemplateMapper.selectById(binding.getTemplateId());
            if (template != null && Objects.equals(template.getStatus(), STATUS_ENABLED)) {
                student.setCurrentPermissionTemplateId(template.getId());
                student.setCurrentPermissionTemplateName(template.getTemplateName());
                student.setCurrentPermissionTemplateSource(SOURCE_MANUAL);
                return;
            }
        }

        StuPermissionTemplateEntity autoTemplate = resolveBestMatchedTemplate(student);
        if (autoTemplate != null) {
            student.setCurrentPermissionTemplateId(autoTemplate.getId());
            student.setCurrentPermissionTemplateName(autoTemplate.getTemplateName());
            student.setCurrentPermissionTemplateSource(SOURCE_AUTO);
            return;
        }

        student.setCurrentPermissionTemplateId(null);
        student.setCurrentPermissionTemplateName(null);
        student.setCurrentPermissionTemplateSource(SOURCE_NONE);
    }

    private StudentEntity requireStudent(Long studentId) {
        StudentEntity student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学员不存在");
        }
        return student;
    }

    private StuPermissionTemplateEntity requireActiveTemplate(Long templateId) {
        StuPermissionTemplateEntity template = stuPermissionTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("权限模板不存在");
        }
        if (!Objects.equals(template.getStatus(), STATUS_ENABLED)) {
            throw new BusinessException("权限模板未启用，无法绑定");
        }
        return template;
    }

    private StuStudentPermissionTemplateEntity resolveActiveBinding(Long studentId) {
        Date now = new Date();
        return stuStudentPermissionTemplateMapper.selectByStudentId(studentId).stream()
                .filter(item -> item.getStatus() != null && item.getStatus() == STATUS_ENABLED)
                .filter(item -> item.getEffectiveStartTime() == null || !item.getEffectiveStartTime().after(now))
                .filter(item -> item.getEffectiveEndTime() == null || !item.getEffectiveEndTime().before(now))
                .findFirst()
                .orElse(null);
    }

    private StuPermissionTemplateEntity resolveBestMatchedTemplate(StudentEntity student) {
        return stuPermissionTemplateMapper.selectAllActive().stream()
                .filter(item -> matches(item.getStudyMode(), student.getStudyMode()))
                .filter(item -> matches(item.getProductId(), student.getProductId()))
                .filter(item -> matches(item.getClassId(), student.getClassId()))
                .sorted(Comparator
                        .comparingInt((StuPermissionTemplateEntity item) -> item.getClassId() == null ? 0 : 1).reversed()
                        .thenComparingInt(item -> item.getProductId() == null ? 0 : 1).reversed()
                        .thenComparingInt(item -> item.getStudyMode() == null ? 0 : 1).reversed()
                        .thenComparing(StuPermissionTemplateEntity::getId))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(Object templateValue, Object studentValue) {
        return templateValue == null || Objects.equals(templateValue, studentValue);
    }

    private void validateEffectiveTime(Date startTime, Date endTime) {
        if (startTime != null && endTime != null && endTime.before(startTime)) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
    }

    private Integer normalizeStatus(Integer status) {
        return Objects.equals(status, 0) ? 0 : STATUS_ENABLED;
    }

    private StudentPermissionTemplateBindingRes toBindingRes(StuStudentPermissionTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        StudentPermissionTemplateBindingRes res = new StudentPermissionTemplateBindingRes();
        res.setId(entity.getId());
        res.setStudentId(entity.getStudentId());
        res.setTemplateId(entity.getTemplateId());
        res.setEffectiveStartTime(entity.getEffectiveStartTime());
        res.setEffectiveEndTime(entity.getEffectiveEndTime());
        res.setStatus(entity.getStatus());
        res.setCreatedAt(entity.getCreatedAt());
        res.setUpdatedAt(entity.getUpdatedAt());
        StuPermissionTemplateEntity template = stuPermissionTemplateMapper.selectById(entity.getTemplateId());
        if (template != null) {
            res.setTemplateCode(template.getTemplateCode());
            res.setTemplateName(template.getTemplateName());
        }
        return res;
    }
}
