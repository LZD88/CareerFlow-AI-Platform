package cn.yq.modules.studentportal.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import cn.yq.modules.studentportal.domain.vo.req.StuAssignPermissionReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateCreateReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateQueryReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateUpdateReq;
import cn.yq.modules.studentportal.domain.vo.res.StuPermissionTemplateDetailRes;
import cn.yq.modules.studentportal.service.StuPermissionTemplateManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/student-permission-templates")
public class StuPermissionTemplateController {

    private final StuPermissionTemplateManageService stuPermissionTemplateManageService;

    @GetMapping
    public ApiResponse<PageResult<StuPermissionTemplateEntity>> page(StuPermissionTemplateQueryReq request) {
        return ApiResponse.success(stuPermissionTemplateManageService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StuPermissionTemplateDetailRes> detail(@PathVariable Long id) {
        return ApiResponse.success(stuPermissionTemplateManageService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid StuPermissionTemplateCreateReq request) {
        return ApiResponse.success("创建成功", stuPermissionTemplateManageService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid StuPermissionTemplateUpdateReq request) {
        stuPermissionTemplateManageService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        stuPermissionTemplateManageService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> assignPermissions(@PathVariable Long id, @RequestBody @Valid StuAssignPermissionReq request) {
        stuPermissionTemplateManageService.assignPermissions(id, request);
        return ApiResponse.success("权限分配成功", null);
    }
}
