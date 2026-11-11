package cn.yq.modules.interviewreview.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewResumeConfirmReq {

    @NotBlank(message = "简历文件名不能为空")
    private String resumeFilename;

    @NotBlank(message = "简历对象 Key 不能为空")
    private String resumeObjectKey;

    @NotBlank(message = "简历地址不能为空")
    private String resumeUrl;

    private String resumeContentType;

    @NotNull(message = "简历大小不能为空")
    private Long resumeSize;
}
