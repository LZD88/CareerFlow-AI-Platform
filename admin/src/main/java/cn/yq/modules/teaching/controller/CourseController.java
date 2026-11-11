package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.entity.CourseEntity;
import cn.yq.modules.teaching.domain.vo.req.CourseCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CourseQueryReq;
import cn.yq.modules.teaching.domain.vo.req.CourseUpdateReq;
import cn.yq.modules.teaching.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/courses")
public class CourseController {

    private final CourseService courseService;

    /**
     * 课程分页查询。
     */
    @GetMapping
    public ApiResponse<PageResult<CourseEntity>> page(CourseQueryReq request) {
        return ApiResponse.success(courseService.page(request));
    }

    /**
     * 查询课程详情，供编辑弹窗回填使用。
     */
    @GetMapping("/{id}")
    public ApiResponse<CourseEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(courseService.detail(id));
    }

    /**
     * 创建课程。
     */
    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CourseCreateReq request) {
        return ApiResponse.success("创建成功", courseService.create(request));
    }

    /**
     * 更新课程基础信息。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid CourseUpdateReq request) {
        courseService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 删除课程。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
