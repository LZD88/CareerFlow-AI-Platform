package cn.yq.modules.studentresume.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.studentresume.domain.vo.req.StudentResumeSaveReq;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeRes;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeUploadRes;
import cn.yq.modules.studentresume.service.StudentResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/student-api/resumes")
public class StudentPortalResumeController {

    private final StudentResumeService studentResumeService;

    @GetMapping
    public ApiResponse<List<StudentResumeRes>> list() {
        return ApiResponse.success(studentResumeService.studentList());
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResumeRes> detail(@PathVariable Long id) {
        return ApiResponse.success(studentResumeService.studentDetail(id));
    }

    @PostMapping
    public ApiResponse<StudentResumeRes> create(@RequestBody @Valid StudentResumeSaveReq request) {
        return ApiResponse.success("简历已保存", studentResumeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentResumeRes> update(@PathVariable Long id, @RequestBody @Valid StudentResumeSaveReq request) {
        return ApiResponse.success("简历已更新", studentResumeService.update(id, request));
    }

    @PostMapping("/upload")
    public ApiResponse<StudentResumeUploadRes> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("简历已上传并解析", studentResumeService.uploadAndParse(file));
    }

    @PutMapping("/{id}/default")
    public ApiResponse<Void> setDefault(@PathVariable Long id) {
        studentResumeService.setDefault(id);
        return ApiResponse.success("已设为默认简历");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentResumeService.studentDelete(id);
        return ApiResponse.success("简历已删除");
    }
}
