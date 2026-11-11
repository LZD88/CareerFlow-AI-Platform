package cn.yq.modules.interviewreview.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewAudioPresignReq {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    private String contentType;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    /**
     * 文件类型：AUDIO 面试音频 / RESUME 简历文件。
     */
    private String fileCategory = "AUDIO";
}
