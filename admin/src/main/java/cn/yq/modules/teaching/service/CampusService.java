package cn.yq.modules.teaching.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.entity.CampusEntity;
import cn.yq.modules.teaching.domain.vo.req.CampusCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CampusQueryReq;
import cn.yq.modules.teaching.domain.vo.req.CampusUpdateReq;

public interface CampusService {

    PageResult<CampusEntity> page(CampusQueryReq request);

    CampusEntity detail(Long id);

    Long create(CampusCreateReq request);

    void update(Long id, CampusUpdateReq request);

    void delete(Long id);
}
