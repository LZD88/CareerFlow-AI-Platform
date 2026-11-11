package cn.yq.modules.studentresume.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.studentresume.domain.vo.req.StudentResumeQueryReq;
import cn.yq.modules.studentresume.domain.vo.req.StudentResumeSaveReq;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeRes;
import cn.yq.modules.studentresume.domain.vo.res.StudentResumeUploadRes;
import cn.yq.modules.upload.domains.vo.res.TosFileContentRes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentResumeService {

    List<StudentResumeRes> studentList();

    StudentResumeRes studentDetail(Long id);

    StudentResumeRes create(StudentResumeSaveReq request);

    StudentResumeRes update(Long id, StudentResumeSaveReq request);

    StudentResumeUploadRes uploadAndParse(MultipartFile file);

    void setDefault(Long id);

    void studentDelete(Long id);

    PageResult<StudentResumeRes> adminPage(StudentResumeQueryReq request);

    StudentResumeRes adminDetail(Long id);

    TosFileContentRes adminPreviewFile(Long id);

    void adminDelete(Long id);
}
