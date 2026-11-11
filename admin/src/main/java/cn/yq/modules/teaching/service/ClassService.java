package cn.yq.modules.teaching.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.domain.vo.req.ClassCreateReq;
import cn.yq.modules.teaching.domain.vo.req.ClassQueryReq;
import cn.yq.modules.teaching.domain.vo.req.ClassUpdateReq;

public interface ClassService {

    PageResult<ClassEntity> page(ClassQueryReq request);

    ClassEntity detail(Long id);

    Long create(ClassCreateReq request);

    void update(Long id, ClassUpdateReq request);

    void delete(Long id);
}
