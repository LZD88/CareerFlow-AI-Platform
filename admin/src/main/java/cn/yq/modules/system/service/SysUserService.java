package cn.yq.modules.system.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.system.domain.vo.req.AssignRoleReq;
import cn.yq.modules.system.domain.vo.req.UserCreateReq;
import cn.yq.modules.system.domain.vo.req.UserQueryReq;
import cn.yq.modules.system.domain.vo.req.UserUpdateReq;
import cn.yq.modules.system.domain.vo.res.UserDetailRes;

/**
 * 用户服务接口。
 */
public interface SysUserService {

    PageResult<SysUserEntity> page(UserQueryReq request);

    Long create(UserCreateReq request);

    void update(Long id, UserUpdateReq request);

    UserDetailRes detail(Long id);

    void delete(Long id);

    void assignRoles(Long userId, AssignRoleReq request);
}
