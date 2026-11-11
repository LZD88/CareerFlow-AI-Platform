package cn.yq.modules.teaching.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.entity.CourseEntity;
import cn.yq.modules.teaching.domain.vo.req.CourseCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CourseQueryReq;
import cn.yq.modules.teaching.domain.vo.req.CourseUpdateReq;

public interface CourseService {

    PageResult<CourseEntity> page(CourseQueryReq request);

    CourseEntity detail(Long id);

    Long create(CourseCreateReq request);

    void update(Long id, CourseUpdateReq request);

    void delete(Long id);
}
