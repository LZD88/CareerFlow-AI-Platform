package cn.yq.modules.system.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.vo.req.PermissionCreateReq;
import cn.yq.modules.system.domain.vo.req.PermissionQueryReq;
import cn.yq.modules.system.domain.vo.req.PermissionUpdateReq;
import cn.yq.modules.system.domain.vo.res.PermissionTreeRes;
import cn.yq.modules.studentportal.domain.vo.res.StuPermissionTreeRes;
import cn.yq.modules.system.service.SysPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/permissions")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    @GetMapping
    public ApiResponse<PageResult<SysPermissionEntity>> page(PermissionQueryReq request) {
        return ApiResponse.success(sysPermissionService.page(request));
    }

    @GetMapping("/tree")
    public ApiResponse<java.util.List<PermissionTreeRes>> tree() {
        return ApiResponse.success(sysPermissionService.tree());
    }

    @GetMapping("/student/tree")
    public ApiResponse<java.util.List<StuPermissionTreeRes>> studentTree() {
        return ApiResponse.success(sysPermissionService.studentTree());
    }

    @GetMapping("/{id}")
    public ApiResponse<SysPermissionEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(sysPermissionService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid PermissionCreateReq request) {
        return ApiResponse.success("创建成功", sysPermissionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid PermissionUpdateReq request) {
        sysPermissionService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sysPermissionService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
