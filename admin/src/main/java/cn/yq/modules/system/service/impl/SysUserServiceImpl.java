package cn.yq.modules.system.service.impl;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.system.domain.vo.req.AssignRoleReq;
import cn.yq.modules.system.domain.vo.req.UserCreateReq;
import cn.yq.modules.system.domain.vo.req.UserQueryReq;
import cn.yq.modules.system.domain.vo.req.UserUpdateReq;
import cn.yq.modules.system.domain.vo.res.UserDetailRes;
import cn.yq.modules.system.mapper.SysRoleMapper;
import cn.yq.modules.system.mapper.SysUserMapper;
import cn.yq.modules.system.service.SysUserService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 用户管理服务实现。
 * 当前权限模型只保留“用户-角色-权限”链路，不支持用户直授权限。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public PageResult<SysUserEntity> page(UserQueryReq request) {
        Page<SysUserEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysUserEntity> records = sysUserMapper.selectPage(
                request.getKeyword(),
                request.getStatus(),
                request.getRoleCode()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateReq request) {
        if (sysUserMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUserEntity user = new SysUserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        sysUserMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateReq request) {
        SysUserEntity existed = requireUser(id);
        existed.setNickname(request.getNickname());
        existed.setRealName(request.getRealName());
        existed.setPhone(request.getPhone());
        existed.setEmail(request.getEmail());
        existed.setStatus(request.getStatus());
        existed.setUpdatedAt(new Date());
        sysUserMapper.updateById(existed);
    }

    @Override
    public UserDetailRes detail(Long id) {
        SysUserEntity user = requireUser(id);
        UserDetailRes res = new UserDetailRes();
        res.setUser(user);
        res.setRoles(sysUserMapper.selectRolesByUserId(id));
        res.setRolePermissions(sysUserMapper.selectRolePermissionsByUserId(id));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireUser(id);
        sysUserMapper.deleteUserRoles(id);
        sysUserMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, AssignRoleReq request) {
        requireUser(userId);
        request.getRoleIds().forEach(roleId -> {
            if (sysRoleMapper.selectById(roleId) == null) {
                throw new BusinessException("角色不存在: " + roleId);
            }
        });
        sysUserMapper.deleteUserRoles(userId);
        sysUserMapper.insertUserRoles(userId, request.getRoleIds());
    }

    private SysUserEntity requireUser(Long id) {
        SysUserEntity user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
