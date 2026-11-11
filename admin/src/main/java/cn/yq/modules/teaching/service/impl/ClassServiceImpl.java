package cn.yq.modules.teaching.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.mapper.SysUserMapper;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.domain.vo.req.ClassCreateReq;
import cn.yq.modules.teaching.domain.vo.req.ClassQueryReq;
import cn.yq.modules.teaching.domain.vo.req.ClassUpdateReq;
import cn.yq.modules.teaching.enums.ClassStatusEnum;
import cn.yq.modules.teaching.mapper.ClassMapper;
import cn.yq.modules.teaching.mapper.CampusMapper;
import cn.yq.modules.teaching.mapper.CourseMapper;
import cn.yq.modules.teaching.service.ClassService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassMapper classMapper;
    private final CampusMapper campusMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseMapper courseMapper;

    @Override
    public PageResult<ClassEntity> page(ClassQueryReq request) {
        Page<ClassEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<ClassEntity> records = classMapper.selectPage(
                request.getKeyword(),
                request.getClassStatus(),
                request.getCampusId(),
                request.getHeadTeacherId(),
                request.getCourseId()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public ClassEntity detail(Long id) {
        return requireClass(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ClassCreateReq request) {
        validateReference(request.getCampusId(), request.getHeadTeacherId(), request.getCourseId(), request.getClassStatus());
        ClassEntity entity = new ClassEntity();
        entity.setClassTerm(request.getClassTerm());
        entity.setCampusId(request.getCampusId());
        entity.setHeadTeacherId(request.getHeadTeacherId());
        entity.setClassStatus(request.getClassStatus());
        entity.setStartTime(request.getStartTime());
        entity.setCourseId(request.getCourseId());
        entity.setStudentCount(request.getStudentCount());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        classMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ClassUpdateReq request) {
        validateReference(request.getCampusId(), request.getHeadTeacherId(), request.getCourseId(), request.getClassStatus());
        ClassEntity entity = requireClass(id);
        entity.setClassTerm(request.getClassTerm());
        entity.setCampusId(request.getCampusId());
        entity.setHeadTeacherId(request.getHeadTeacherId());
        entity.setClassStatus(request.getClassStatus());
        entity.setStartTime(request.getStartTime());
        entity.setCourseId(request.getCourseId());
        entity.setStudentCount(request.getStudentCount());
        entity.setUpdatedAt(new Date());
        classMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireClass(id);
        classMapper.deleteById(id);
    }

    private void validateReference(Long campusId, Long headTeacherId, Long courseId, String classStatus) {
        if (campusMapper.selectById(campusId) == null) {
            throw new BusinessException("校区不存在");
        }
        if (sysUserMapper.selectById(headTeacherId) == null) {
            throw new BusinessException("班主任不存在");
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException("课程不存在");
        }
        ClassStatusEnum.validate(classStatus);
    }

    private ClassEntity requireClass(Long id) {
        ClassEntity entity = classMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("班级不存在");
        }
        return entity;
    }
}
