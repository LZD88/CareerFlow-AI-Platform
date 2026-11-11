package cn.yq.modules.system.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.vo.req.PermissionCreateReq;
import cn.yq.modules.system.domain.vo.req.PermissionQueryReq;
import cn.yq.modules.system.domain.vo.req.PermissionUpdateReq;
import cn.yq.modules.system.domain.vo.res.PermissionTreeRes;
import cn.yq.modules.studentportal.domain.vo.res.StuPermissionTreeRes;

import java.util.List;

/**
 * 权限服务接口。
 */
public interface SysPermissionService {

    PageResult<SysPermissionEntity> page(PermissionQueryReq request);

    List<PermissionTreeRes> tree();

    List<StuPermissionTreeRes> studentTree();

    Long create(PermissionCreateReq request);

    void update(Long id, PermissionUpdateReq request);

    SysPermissionEntity detail(Long id);

    void delete(Long id);
}
