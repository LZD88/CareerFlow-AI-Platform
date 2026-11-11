package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.entity.ClassEntity;
import cn.yq.modules.teaching.domain.vo.req.ClassCreateReq;
import cn.yq.modules.teaching.domain.vo.req.ClassQueryReq;
import cn.yq.modules.teaching.domain.vo.req.ClassUpdateReq;
import cn.yq.modules.teaching.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/classes")
public class ClassController {

    private final ClassService classService;

    @GetMapping
    public ApiResponse<PageResult<ClassEntity>> page(ClassQueryReq request) {
        return ApiResponse.success(classService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClassEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(classService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid ClassCreateReq request) {
        return ApiResponse.success("创建成功", classService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid ClassUpdateReq request) {
        classService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
