package cn.yq.modules.teaching.support;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.teaching.domain.entity.CourseEntity;
import cn.yq.modules.teaching.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 课程领域支撑组件。
 * 统一承接课程存在性校验等公共能力，避免 service 内部自调用。
 */
@Component
@RequiredArgsConstructor
public class CourseSupport {

    private final CourseMapper courseMapper;

    public CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }
}
