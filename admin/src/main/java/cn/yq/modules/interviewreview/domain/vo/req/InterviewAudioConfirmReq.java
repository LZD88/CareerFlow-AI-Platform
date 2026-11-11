package cn.yq.modules.interviewreview.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewAudioConfirmReq {

    private String title;

    private String candidateName;

    private String interviewRole;

    @NotBlank(message = "文件名不能为空")
    private String originalFilename;

    @NotBlank(message = "音频对象 Key 不能为空")
    private String audioObjectKey;

    @NotBlank(message = "音频地址不能为空")
    private String audioUrl;

    private String audioContentType;

    @NotNull(message = "文件大小不能为空")
    private Long audioSize;

    private String resumeFilename;

    private String resumeObjectKey;

    private String resumeUrl;

    private String resumeContentType;

    private Long resumeSize;
}
