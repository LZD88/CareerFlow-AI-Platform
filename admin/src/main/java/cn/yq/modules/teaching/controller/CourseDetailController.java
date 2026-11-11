package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.teaching.biz.CourseDetailBiz;
import cn.yq.modules.teaching.domain.entity.CourseDetailEntity;
import cn.yq.modules.teaching.domain.vo.req.CourseDetailCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CourseDetailUpdateReq;
import cn.yq.modules.teaching.service.CourseDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/courses/{courseId}/details")
public class CourseDetailController {

    private final CourseDetailBiz courseDetailBiz;
    private final CourseDetailService courseDetailService;

    /**
     * 查询某个课程下的全部课程详情。
     */
    @GetMapping
    public ApiResponse<List<CourseDetailEntity>> list(@PathVariable Long courseId) {
        return ApiResponse.success(courseDetailService.listByCourseId(courseId));
    }

    /**
     * 查询某条课程详情，供编辑弹窗回填使用。
     */
    @GetMapping("/{id}")
    public ApiResponse<CourseDetailEntity> detail(@PathVariable Long courseId, @PathVariable Long id) {
        return ApiResponse.success(courseDetailService.detail(courseId, id));
    }

    /**
     * 新增课程详情。
     */
    @PostMapping
    public ApiResponse<Long> create(@PathVariable Long courseId, @RequestBody @Valid CourseDetailCreateReq request) {
        return ApiResponse.success("创建成功", courseDetailBiz.create(courseId, request));
    }

    /**
     * 编辑课程详情。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long courseId, @PathVariable Long id, @RequestBody @Valid CourseDetailUpdateReq request) {
        courseDetailService.update(courseId, id, request);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 删除单条课程详情。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long courseId, @PathVariable Long id) {
        courseDetailBiz.delete(courseId, id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 删除课程下的全部详情记录。
     */
    @DeleteMapping
    public ApiResponse<Void> deleteAll(@PathVariable Long courseId) {
        courseDetailService.deleteAll(courseId);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 通过 Excel 导入课程详情。
     */
    @PostMapping("/import")
    public ApiResponse<Integer> importExcel(@PathVariable Long courseId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("导入成功", courseDetailBiz.importExcel(courseId, file));
    }
}
