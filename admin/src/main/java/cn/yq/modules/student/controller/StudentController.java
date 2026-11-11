package cn.yq.modules.student.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.student.domain.vo.req.StudentBindPermissionTemplateReq;
import cn.yq.modules.student.domain.vo.req.StudentDailyMomentAdminQueryReq;
import cn.yq.modules.student.domain.vo.req.StudentQueryReq;
import cn.yq.modules.student.domain.vo.res.StudentDailyMomentAdminItemRes;
import cn.yq.modules.student.domain.vo.res.StudentPermissionTemplateDetailRes;
import cn.yq.modules.student.service.StudentDailyMomentManageService;
import cn.yq.modules.student.service.StudentService;
import cn.yq.modules.studentportal.service.StudentPermissionTemplateBindingManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final StudentDailyMomentManageService studentDailyMomentManageService;
    private final StudentPermissionTemplateBindingManageService studentPermissionTemplateBindingManageService;

    @GetMapping
    public ApiResponse<PageResult<StudentEntity>> page(StudentQueryReq request) {
        return ApiResponse.success(studentService.page(request));
    }

    @GetMapping("/daily-moments")
    public ApiResponse<PageResult<StudentDailyMomentAdminItemRes>> dailyMomentPage(StudentDailyMomentAdminQueryReq request) {
        return ApiResponse.success(studentDailyMomentManageService.page(request));
    }

    @GetMapping("/{id}/permission-template-detail")
    public ApiResponse<StudentPermissionTemplateDetailRes> permissionTemplateDetail(@PathVariable Long id) {
        return ApiResponse.success(studentPermissionTemplateBindingManageService.detail(id));
    }

    @PutMapping("/{id}/permission-template")
    public ApiResponse<Void> bindPermissionTemplate(@PathVariable Long id,
                                                    @RequestBody @Valid StudentBindPermissionTemplateReq request) {
        studentPermissionTemplateBindingManageService.bind(id, request);
        return ApiResponse.success("绑定成功");
    }

    @DeleteMapping("/{id}/permission-template")
    public ApiResponse<Void> unbindPermissionTemplate(@PathVariable Long id) {
        studentPermissionTemplateBindingManageService.unbind(id);
        return ApiResponse.success("已恢复自动匹配");
    }
}
