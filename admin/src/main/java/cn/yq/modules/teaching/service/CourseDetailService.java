package cn.yq.modules.teaching.service;

import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import cn.yq.modules.teaching.domain.vo.req.CourseDetailUpdateReq;

import java.util.List;

public interface CourseDetailService {

    List<CourseDetailEntity> listByCourseId(Long courseId);

    CourseDetailEntity detail(Long courseId, Long id);

    Long create(CourseDetailEntity detail);

    void createBatch(List<CourseDetailEntity> details);

    void update(Long courseId, Long id, CourseDetailUpdateReq request);

    void updateBatch(List<CourseDetailEntity> details);

    void increaseDayNumFrom(Long courseId, Integer dayNum);

    void decreaseDayNumAfter(Long courseId, Integer dayNum);

    void delete(Long courseId, Long id);

    void deleteAll(Long courseId);
}
