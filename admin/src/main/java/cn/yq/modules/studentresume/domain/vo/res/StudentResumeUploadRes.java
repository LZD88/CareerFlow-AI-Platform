package cn.yq.modules.studentresume.domain.vo.res;

import lombok.Data;

@Data
public class StudentResumeUploadRes {

    private String originalFileName;

    private String resumeFileUrl;

    private String contentType;

    private String resumeText;

    private String parseStatus;
}
