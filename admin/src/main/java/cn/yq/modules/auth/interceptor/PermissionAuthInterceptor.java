package cn.yq.modules.auth.interceptor;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.entity.SysRolePermissionEntity;
import cn.yq.modules.system.domain.entity.SysUserRoleEntity;
import cn.yq.modules.system.mapper.SysPermissionMapper;
import cn.yq.modules.system.mapper.SysRolePermissionMapper;
import cn.yq.modules.system.mapper.SysUserMapper;
import cn.yq.modules.system.mapper.SysUserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 接口权限拦截器。
 * 在JWT校验通过后，使用分步单表查询方式判断当前用户是否允许访问接口。
 */
@Component
@RequiredArgsConstructor
public class PermissionAuthInterceptor implements HandlerInterceptor {

    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        Object loginUserIdAttr = request.getAttribute("loginUserId");
        if (loginUserIdAttr == null) {
            throw new BusinessException(401, "未登录或登录信息失效");
        }

        Long loginUserId = Long.parseLong(loginUserIdAttr.toString());
        String requestUri = request.getRequestURI();
        String applicationPath = requestUri.substring(request.getContextPath().length());

        if (isSuperAdmin(loginUserId)) {
            return true;
        }

        List<SysPermissionEntity> permissions = loadApiPermissions(loginUserId);
        boolean matched = permissions.stream()
                .map(SysPermissionEntity::getApiPath)
                .filter(Objects::nonNull)
                .filter(path -> !path.isBlank())
                .anyMatch(path -> antPathMatcher.match(path, applicationPath) || antPathMatcher.match(path, requestUri));

        if (!matched) {
            throw new BusinessException(403, "无权访问当前接口");
        }
        return true;
    }

    private boolean isSuperAdmin(Long loginUserId) {
        List<SysRoleEntity> roles = sysUserMapper.selectRolesByUserId(loginUserId);
        return roles.stream().map(SysRoleEntity::getRoleCode).anyMatch(SUPER_ADMIN_ROLE_CODE::equals);
    }

    private List<SysPermissionEntity> loadApiPermissions(Long loginUserId) {
        List<SysUserRoleEntity> userRoles = sysUserRoleMapper.selectByUserId(loginUserId);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRoleEntity::getRoleId)
                .distinct()
                .toList();
        List<SysRolePermissionEntity> rolePermissions = sysRolePermissionMapper.selectByRoleIds(roleIds);
        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermissionEntity::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        return sysPermissionMapper.selectAllByIds(permissionIds).stream()
                .filter(permission -> "API".equals(permission.getPermissionType()))
                .filter(permission -> permission.getStatus() != null && permission.getStatus() == 1)
                .collect(Collectors.toList());
    }
}
