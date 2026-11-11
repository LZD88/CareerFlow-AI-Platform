package cn.yq.modules.system.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.system.domain.entity.SysUserEntity;
import cn.yq.modules.system.domain.vo.req.AssignRoleReq;
import cn.yq.modules.system.domain.vo.req.UserCreateReq;
import cn.yq.modules.system.domain.vo.req.UserQueryReq;
import cn.yq.modules.system.domain.vo.req.UserUpdateReq;
import cn.yq.modules.system.domain.vo.res.UserDetailRes;
import cn.yq.modules.system.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/users")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    public ApiResponse<PageResult<SysUserEntity>> page(UserQueryReq request) {
        return ApiResponse.success(sysUserService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailRes> detail(@PathVariable Long id) {
        return ApiResponse.success(sysUserService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid UserCreateReq request) {
        return ApiResponse.success("创建成功", sysUserService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid UserUpdateReq request) {
        sysUserService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<Void> assignRoles(@PathVariable Long id, @RequestBody @Valid AssignRoleReq request) {
        sysUserService.assignRoles(id, request);
        return ApiResponse.success("分配角色成功", null);
    }
}
