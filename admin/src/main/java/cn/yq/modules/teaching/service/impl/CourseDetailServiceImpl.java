package cn.yq.modules.teaching.service.impl;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import cn.yq.modules.teaching.domain.vo.req.CourseDetailUpdateReq;
import cn.yq.modules.teaching.mapper.CourseDetailMapper;
import cn.yq.modules.teaching.service.CourseDetailService;
import cn.yq.modules.teaching.support.CourseSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 课程详情管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class CourseDetailServiceImpl implements CourseDetailService {

    private final CourseDetailMapper courseDetailMapper;
    private final CourseSupport courseSupport;

    @Override
    public List<CourseDetailEntity> listByCourseId(Long courseId) {
        courseSupport.requireCourse(courseId);
        return courseDetailMapper.selectByCourseId(courseId);
    }

    @Override
    public CourseDetailEntity detail(Long courseId, Long id) {
        courseSupport.requireCourse(courseId);
        return requireDetail(courseId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CourseDetailEntity detail) {
        courseDetailMapper.insert(detail);
        return detail.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(List<CourseDetailEntity> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        courseDetailMapper.insertBatch(details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long courseId, Long id, CourseDetailUpdateReq request) {
        courseSupport.requireCourse(courseId);
        CourseDetailEntity detail = requireDetail(courseId, id);
        detail.setStageName(request.getStageName());
        detail.setDayNum(request.getDayNum());
        detail.setCourseContent(request.getCourseContent());
        detail.setUpdatedAt(new Date());
        courseDetailMapper.updateById(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBatch(List<CourseDetailEntity> details) {
        if (details == null || details.isEmpty()) {
            return;
        }
        courseDetailMapper.updateBatchById(details);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseDayNumFrom(Long courseId, Integer dayNum) {
        courseSupport.requireCourse(courseId);
        courseDetailMapper.increaseDayNumFrom(courseId, dayNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseDayNumAfter(Long courseId, Integer dayNum) {
        courseSupport.requireCourse(courseId);
        courseDetailMapper.decreaseDayNumAfter(courseId, dayNum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long courseId, Long id) {
        courseSupport.requireCourse(courseId);
        requireDetail(courseId, id);
        courseDetailMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(Long courseId) {
        courseSupport.requireCourse(courseId);
        courseDetailMapper.deleteByCourseId(courseId);
    }

    private CourseDetailEntity requireDetail(Long courseId, Long id) {
        CourseDetailEntity detail = courseDetailMapper.selectById(id);
        if (detail == null || !courseId.equals(detail.getCourseId())) {
            throw new BusinessException("课程详情不存在");
        }
        return detail;
    }
}
