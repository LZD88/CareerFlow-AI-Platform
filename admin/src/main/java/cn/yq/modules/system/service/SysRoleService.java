package cn.yq.modules.system.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.vo.req.AssignPermissionReq;
import cn.yq.modules.system.domain.vo.req.RoleCreateReq;
import cn.yq.modules.system.domain.vo.req.RoleQueryReq;
import cn.yq.modules.system.domain.vo.req.RoleUpdateReq;
import cn.yq.modules.system.domain.vo.res.RoleDetailRes;

/**
 * 角色服务接口。
 */
public interface SysRoleService {

    PageResult<SysRoleEntity> page(RoleQueryReq request);

    Long create(RoleCreateReq request);

    void update(Long id, RoleUpdateReq request);

    RoleDetailRes detail(Long id);

    void delete(Long id);

    void assignPermissions(Long roleId, AssignPermissionReq request);
}
