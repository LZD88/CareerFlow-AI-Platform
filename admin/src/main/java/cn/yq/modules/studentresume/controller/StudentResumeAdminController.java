package cn.yq.modules.studentresume.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.studentresume.domain.vo.req.StudentResumeQueryReq;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeRes;
import cn.yq.modules.studentresume.service.StudentResumeService;
import cn.yq.modules.upload.domains.vo.res.TosFileContentRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/student-resumes")
public class StudentResumeAdminController {

    private final StudentResumeService studentResumeService;

    @GetMapping
    public ApiResponse<PageResult<StudentResumeRes>> page(StudentResumeQueryReq request) {
        return ApiResponse.success(studentResumeService.adminPage(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentResumeRes> detail(@PathVariable Long id) {
        return ApiResponse.success(studentResumeService.adminDetail(id));
    }

    @GetMapping("/{id}/file/preview")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long id) {
        TosFileContentRes content = studentResumeService.adminPreviewFile(id);
        String contentType = content.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : content.getContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, content.getContentDisposition())
                .contentType(MediaType.parseMediaType(contentType))
                .body(content.getContent());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentResumeService.adminDelete(id);
        return ApiResponse.success("简历已删除");
    }
}
