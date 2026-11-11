package cn.yq.modules.studentresume.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentResumeSaveReq {

    @NotBlank(message = "简历名称不能为空")
    private String resumeName;

    private String targetRole;

    @NotBlank(message = "简历内容不能为空")
    private String resumeText;

    private String resumeSummary;

    private String resumeFileUrl;

    private Boolean defaultResume;
}
