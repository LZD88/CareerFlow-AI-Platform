package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HomeworkAnswerManageReq {

    private String answerAttachmentName;

    private String answerAttachmentUrl;

    private String answerAttachmentType;

    @NotNull(message = "请指明是否发布答案")
    private Boolean publishAnswer;
}
