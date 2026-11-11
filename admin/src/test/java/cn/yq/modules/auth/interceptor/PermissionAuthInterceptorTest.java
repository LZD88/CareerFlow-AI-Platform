package cn.yq.modules.auth.interceptor;

import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.domain.entity.*;
import cn.yq.modules.system.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionAuthInterceptorTest {
    @Mock SysUserMapper users;
    @Mock SysUserRoleMapper userRoles;
    @Mock SysRolePermissionMapper rolePermissions;
    @Mock SysPermissionMapper permissions;
    @InjectMocks PermissionAuthInterceptor interceptor;

    @Test void matchesSeedPermissionWithoutDeploymentContextPrefix() {
        var request = new MockHttpServletRequest("POST", "/yq-admin/api/sales/orders/1/enrollment");
        request.setContextPath("/yq-admin");
        request.setAttribute("loginUserId", 1L);
        var userRole = new SysUserRoleEntity();
        userRole.setRoleId(2L);
        var rolePermission = new SysRolePermissionEntity();
        rolePermission.setPermissionId(3L);
        var permission = new SysPermissionEntity();
        permission.setApiPath("/api/sales/orders/*/enrollment");
        permission.setPermissionType("API");
        permission.setStatus(1);
        when(users.selectRolesByUserId(1L)).thenReturn(List.of());
        when(userRoles.selectByUserId(1L)).thenReturn(List.of(userRole));
        when(rolePermissions.selectByRoleIds(any())).thenReturn(List.of(rolePermission));
        when(permissions.selectAllByIds(any())).thenReturn(List.of(permission));
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
    @Test void rejectsMissingLoginIdentity() {
        assertThrows(BusinessException.class, () -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
        verifyNoInteractions(users, userRoles, rolePermissions, permissions);
    }
}
