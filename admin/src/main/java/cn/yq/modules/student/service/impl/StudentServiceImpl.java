package cn.yq.modules.student.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.ProductEntity;
import cn.yq.modules.sales.mapper.ProductMapper;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentCreateReq;
import cn.yq.modules.student.domain.vo.req.StudentQueryReq;
import cn.yq.modules.student.domain.vo.req.StudentUpdateReq;
import cn.yq.modules.student.mapper.StudentMapper;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.service.StudentPermissionTemplateBindingManageService;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.mapper.ClassMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String STUDY_MODE_ONLINE = "ONLINE";
    private static final String STUDY_MODE_OFFLINE = "OFFLINE";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String PHONE_PATTERN = "^1\\d{10}$";

    private final StudentMapper studentMapper;
    private final ProductMapper productMapper;
    private final ClassMapper classMapper;
    private final StudentPermissionTemplateBindingManageService studentPermissionTemplateBindingManageService;

    @Override
    public PageResult<StudentEntity> page(StudentQueryReq request) {
        Page<StudentEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<StudentEntity> records = studentMapper.selectPage(
                request.getKeyword(),
                normalizeStudyMode(request.getStudyMode(), false),
                request.getClassId(),
                request.getProductId()
        );
        records.forEach(studentPermissionTemplateBindingManageService::fillEffectiveTemplateSummary);
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public StudentEntity detail(Long id) {
        return requireStudent(id);
    }

    @Override
    public StudentEntity findByStudentCode(String studentCode) {
        if (!StringUtils.hasText(studentCode)) {
            return null;
        }
        return studentMapper.selectByStudentCode(studentCode.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StudentCreateReq request) {
        validateStudentCodeUnique(request.getStudentCode(), null);
        validatePhone(request.getPhone(), null);
        ProductEntity product = requireProduct(request.getProductId());
        ClassEntity classEntity = resolveClass(request.getStudyMode(), request.getClassId());

        StudentEntity entity = new StudentEntity();
        fillStudentEntity(entity, request.getStudentCode(), request.getStudentName(), request.getPhone(), request.getPassword(),
                request.getGraduationSession(), request.getSchool(), request.getEducation(), request.getStudyMode(),
                classEntity == null ? null : classEntity.getId(), product.getId(), request.getJoinTime());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        studentMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StudentUpdateReq request) {
        StudentEntity entity = requireStudent(id);
        validateStudentCodeUnique(request.getStudentCode(), id);
        validatePhone(request.getPhone(), id);
        ProductEntity product = requireProduct(request.getProductId());
        ClassEntity classEntity = resolveClass(request.getStudyMode(), request.getClassId());
        String password = StringUtils.hasText(request.getPassword()) ? request.getPassword() : entity.getPassword();

        fillStudentEntity(entity, request.getStudentCode(), request.getStudentName(), request.getPhone(), password,
                request.getGraduationSession(), request.getSchool(), request.getEducation(), request.getStudyMode(),
                classEntity == null ? null : classEntity.getId(), product.getId(), request.getJoinTime());
        entity.setUpdatedAt(new Date());
        studentMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireStudent(id);
        studentMapper.deleteById(id);
    }

    private StudentEntity requireStudent(Long id) {
        StudentEntity entity = studentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("学员不存在");
        }
        return entity;
    }

    private ProductEntity requireProduct(Long productId) {
        ProductEntity entity = productMapper.selectById(productId);
        if (entity == null) {
            throw new BusinessException("产品不存在");
        }
        return entity;
    }

    private ClassEntity resolveClass(String studyMode, Long classId) {
        String normalizedStudyMode = normalizeStudyMode(studyMode, true);
        if (STUDY_MODE_OFFLINE.equals(normalizedStudyMode)) {
            if (classId == null) {
                throw new BusinessException("线下学员必须选择班级");
            }
            ClassEntity classEntity = classMapper.selectById(classId);
            if (classEntity == null) {
                throw new BusinessException("班级不存在");
            }
            return classEntity;
        }
        return null;
    }

    private void validateStudentCodeUnique(String studentCode, Long currentId) {
        StudentEntity existing = studentMapper.selectByStudentCode(studentCode.trim());
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new BusinessException("学号已存在");
        }
    }

    private void validatePhone(String phone, Long currentId) {
        if (!StringUtils.hasText(phone) || !phone.trim().matches(PHONE_PATTERN)) {
            throw new BusinessException("请输入正确的11位手机号");
        }
        StudentEntity existing = studentMapper.selectByPhone(phone.trim());
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new BusinessException("手机号已存在");
        }
    }

    private String normalizeStudyMode(String studyMode, boolean required) {
        if (!StringUtils.hasText(studyMode)) {
            if (required) {
                throw new BusinessException("学习方式不能为空");
            }
            return null;
        }
        String normalized = studyMode.trim().toUpperCase();
        if (!STUDY_MODE_ONLINE.equals(normalized) && !STUDY_MODE_OFFLINE.equals(normalized)) {
            throw new BusinessException("学习方式仅支持 ONLINE 或 OFFLINE");
        }
        return normalized;
    }

    private void fillStudentEntity(StudentEntity entity,
                                   String studentCode,
                                   String studentName,
                                   String phone,
                                   String password,
                                   Integer graduationSession,
                                   String school,
                                   String education,
                                   String studyMode,
                                   Long classId,
                                   Long productId,
                                   Date joinTime) {
        entity.setStudentCode(studentCode.trim());
        entity.setStudentName(studentName.trim());
        entity.setPhone(phone.trim());
        entity.setPassword(StringUtils.hasText(password) ? password.trim() : DEFAULT_PASSWORD);
        entity.setGraduationSession(graduationSession);
        entity.setSchool(school.trim());
        entity.setEducation(education.trim());
        entity.setStudyMode(normalizeStudyMode(studyMode, true));
        entity.setClassId(classId);
        entity.setProductId(productId);
        entity.setJoinTime(joinTime);
    }
}
