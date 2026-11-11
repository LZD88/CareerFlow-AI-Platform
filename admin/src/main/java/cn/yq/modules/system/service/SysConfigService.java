package cn.yq.modules.system.service;

import cn.yq.common.api.PageResult;
import cn.yq.common.dataConfig.entity.SysConfigEntity;
import cn.yq.modules.system.domain.vo.req.SysConfigCreateReq;
import cn.yq.modules.system.domain.vo.req.SysConfigQueryReq;
import cn.yq.modules.system.domain.vo.req.SysConfigUpdateReq;

public interface SysConfigService {

    PageResult<SysConfigEntity> page(SysConfigQueryReq request);

    SysConfigEntity detail(Long id);

    Long create(SysConfigCreateReq request);

    void update(Long id, SysConfigUpdateReq request);

    void delete(Long id);
}
