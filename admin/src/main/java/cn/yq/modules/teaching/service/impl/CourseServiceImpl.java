package cn.yq.modules.teaching.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.domain.entity.CourseEntity;
import cn.yq.modules.teaching.domain.vo.req.CourseCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CourseQueryReq;
import cn.yq.modules.teaching.domain.vo.req.CourseUpdateReq;
import cn.yq.modules.teaching.mapper.CourseDetailMapper;
import cn.yq.modules.teaching.mapper.CourseMapper;
import cn.yq.modules.teaching.service.CourseService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 课程管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseDetailMapper courseDetailMapper;

    @Override
    public PageResult<CourseEntity> page(CourseQueryReq request) {
        Page<CourseEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<CourseEntity> records = courseMapper.selectPage(request.getKeyword(), request.getStatus());
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public CourseEntity detail(Long id) {
        return requireCourse(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CourseCreateReq request) {
        CourseEntity course = new CourseEntity();
        course.setCourseName(request.getCourseName());
        course.setCourseDays(request.getCourseDays());
        course.setFeishuMaterialPath(request.getFeishuMaterialPath());
        course.setStatus(request.getStatus());
        course.setCreatedAt(new Date());
        course.setUpdatedAt(new Date());
        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CourseUpdateReq request) {
        CourseEntity course = requireCourse(id);
        course.setCourseName(request.getCourseName());
        course.setCourseDays(request.getCourseDays());
        course.setFeishuMaterialPath(request.getFeishuMaterialPath());
        course.setStatus(request.getStatus());
        course.setUpdatedAt(new Date());
        courseMapper.updateById(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCourse(id);
        // 删除课程时同步清理课程详情，避免遗留孤儿数据。
        courseDetailMapper.deleteByCourseId(id);
        courseMapper.deleteById(id);
    }

    private CourseEntity requireCourse(Long id) {
        CourseEntity course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
