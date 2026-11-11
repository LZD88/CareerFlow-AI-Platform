package cn.yq.modules.system.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.vo.req.AssignPermissionReq;
import cn.yq.modules.system.domain.vo.req.RoleCreateReq;
import cn.yq.modules.system.domain.vo.req.RoleQueryReq;
import cn.yq.modules.system.domain.vo.req.RoleUpdateReq;
import cn.yq.modules.system.domain.vo.res.RoleDetailRes;
import cn.yq.modules.system.mapper.SysPermissionMapper;
import cn.yq.modules.system.mapper.SysRoleMapper;
import cn.yq.modules.system.service.SysRoleService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 角色管理服务实现。
 * 负责角色维护以及角色与权限的绑定关系。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public PageResult<SysRoleEntity> page(RoleQueryReq request) {
        Page<SysRoleEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysRoleEntity> records = sysRoleMapper.selectPage(request.getKeyword(), request.getStatus());
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateReq request) {
        if (sysRoleMapper.selectByCode(request.getRoleCode()) != null) {
            throw new BusinessException("角色编码已存在");
        }
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setCreatedAt(new Date());
        role.setUpdatedAt(new Date());
        sysRoleMapper.insert(role);
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleUpdateReq request) {
        SysRoleEntity role = requireRole(id);
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setUpdatedAt(new Date());
        sysRoleMapper.updateById(role);
    }

    @Override
    public RoleDetailRes detail(Long id) {
        SysRoleEntity role = requireRole(id);
        RoleDetailRes res = new RoleDetailRes();
        res.setRole(role);
        res.setPermissions(sysRoleMapper.selectPermissionsByRoleId(id));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireRole(id);
        sysRoleMapper.deleteRolePermissions(id);
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, AssignPermissionReq request) {
        requireRole(roleId);
        validatePermissionIds(request.getPermissionIds());
        sysRoleMapper.deleteRolePermissions(roleId);
        sysRoleMapper.insertRolePermissions(roleId, request.getPermissionIds());
    }

    private SysRoleEntity requireRole(Long id) {
        SysRoleEntity role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    private void validatePermissionIds(List<Long> permissionIds) {
        if (sysPermissionMapper.selectAllByIds(permissionIds).size() != permissionIds.size()) {
            throw new BusinessException("存在无效权限ID");
        }
    }
}
