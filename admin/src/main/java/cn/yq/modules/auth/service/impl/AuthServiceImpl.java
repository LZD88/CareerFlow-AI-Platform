package cn.yq.modules.auth.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.AuthProperties;
import cn.yq.config.SignProperties;
import cn.yq.modules.auth.domain.vo.req.ChangePasswordReq;
import cn.yq.modules.auth.domain.vo.req.FeishuBindReq;
import cn.yq.modules.auth.domain.vo.req.LoginReq;
import cn.yq.modules.auth.domain.vo.res.AuthProfileRes;
import cn.yq.modules.auth.domain.vo.res.FeishuUserInfo;
import cn.yq.modules.auth.domain.vo.res.LoginRes;
import cn.yq.modules.auth.service.AuthService;
import cn.yq.modules.auth.service.FeishuAuthService;
import cn.yq.modules.auth.util.JwtTokenUtil;
import cn.yq.modules.auth.util.SignKeyRedisUtil;
import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 登录认证服务实现。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthProperties authProperties;
    private final SignProperties signProperties;
    private final SignKeyRedisUtil signKeyRedisUtil;
    private final FeishuAuthService feishuAuthService;

    @Override
    public LoginRes login(LoginReq request) {
        SysUserEntity user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null || !request.getPassword().equals(user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "当前账号已被禁用");
        }
        return buildLoginRes(user);
    }

    @Override
    public LoginRes feishuLogin(String code) {

        FeishuUserInfo userInfo = feishuAuthService.getUserInfoByCode(code);
        if (userInfo == null || userInfo.getUnionId() == null || userInfo.getUnionId().isBlank()) {
            throw new BusinessException(400, "飞书账号信息获取有误");
        }

        SysUserEntity user = sysUserMapper.selectByUnionId(userInfo.getUnionId());
        if (user == null) {
            throw new BusinessException(400, "该飞书账号未绑定系统用户，请先使用账号密码登录后完成绑定");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "当前账号已被禁用");
        }
        return buildLoginRes(user);
    }

    @Override
    public AuthProfileRes profile(Long loginUserId) {
        SysUserEntity user = requireUser(loginUserId);
        List<SysRoleEntity> roles = sysUserMapper.selectRolesByUserId(loginUserId);
        List<SysPermissionEntity> permissions = sysUserMapper.selectRolePermissionsByUserId(loginUserId);

        AuthProfileRes res = new AuthProfileRes();
        res.setUserId(user.getId());
        res.setUsername(user.getUsername());
        res.setNickname(user.getNickname());
        res.setUnionId(user.getUnionId());
        res.setFeishuBound(user.getUnionId() != null && !user.getUnionId().isBlank());
        res.setRoleCodes(roles.stream().map(SysRoleEntity::getRoleCode).toList());
        res.setPermissionCodes(permissions.stream().map(SysPermissionEntity::getPermissionCode).distinct().toList());
        return res;
    }

    @Override
    public void bindFeishu(Long loginUserId, FeishuBindReq request) {
        SysUserEntity loginUser = requireUser(loginUserId);
        FeishuUserInfo userInfo = feishuAuthService.getUserInfoByCode(request.getCode());

        SysUserEntity existed = sysUserMapper.selectByUnionId(userInfo.getUnionId());
        if (existed != null && !existed.getId().equals(loginUserId)) {
            throw new BusinessException(400, "该飞书账号已绑定其他系统用户");
        }

        sysUserMapper.updateUnionId(loginUserId, userInfo.getUnionId());
        loginUser.setUnionId(userInfo.getUnionId());
    }

    @Override
    public void unbindFeishu(Long loginUserId) {
        requireUser(loginUserId);
        sysUserMapper.updateUnionId(loginUserId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long loginUserId, ChangePasswordReq request) {
        SysUserEntity user = requireUser(loginUserId);
        if (!user.getPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "旧密码不正确");
        }
        if (request.getNewPassword().length() < 6) {
            throw new BusinessException(400, "新密码长度不能少于6位");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }

        sysUserMapper.updatePassword(loginUserId, request.getNewPassword());
    }

    private LoginRes buildLoginRes(SysUserEntity user) {
        List<SysRoleEntity> roles = sysUserMapper.selectRolesByUserId(user.getId());
        List<SysPermissionEntity> permissions = sysUserMapper.selectRolePermissionsByUserId(user.getId());
        String signSecret = IdUtil.fastSimpleUUID();
        signKeyRedisUtil.cacheUserSecret(user.getId(), signSecret, signProperties.getSecretExpireSeconds());

        LoginRes res = new LoginRes();
        res.setToken(jwtTokenUtil.createToken(user));
        res.setExpiresIn(authProperties.getExpireSeconds());
        res.setUserId(user.getId());
        res.setUsername(user.getUsername());
        res.setNickname(user.getNickname());
        res.setUnionId(user.getUnionId());
        res.setSignSecret(signSecret);
        res.setSignSecretExpiresIn(signProperties.getSecretExpireSeconds());
        res.setRoleCodes(roles.stream().map(SysRoleEntity::getRoleCode).toList());
        res.setPermissionCodes(permissions.stream().map(SysPermissionEntity::getPermissionCode).distinct().toList());
        return res;
    }

    private SysUserEntity requireUser(Long id) {
        SysUserEntity user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
