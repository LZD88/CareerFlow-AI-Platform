package cn.yq.modules.system.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.system.domain.entity.SysRoleEntity;
import cn.yq.modules.system.domain.vo.req.AssignPermissionReq;
import cn.yq.modules.system.domain.vo.req.RoleCreateReq;
import cn.yq.modules.system.domain.vo.req.RoleQueryReq;
import cn.yq.modules.system.domain.vo.req.RoleUpdateReq;
import cn.yq.modules.system.domain.vo.res.RoleDetailRes;
import cn.yq.modules.system.service.SysRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/roles")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping
    public ApiResponse<PageResult<SysRoleEntity>> page(RoleQueryReq request) {
        return ApiResponse.success(sysRoleService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleDetailRes> detail(@PathVariable Long id) {
        return ApiResponse.success(sysRoleService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid RoleCreateReq request) {
        return ApiResponse.success("创建成功", sysRoleService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid RoleUpdateReq request) {
        sysRoleService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sysRoleService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id,
                                               @RequestBody @Valid AssignPermissionReq request) {
        sysRoleService.assignPermissions(id, request);
        return ApiResponse.success("角色授权成功", null);
    }
}
