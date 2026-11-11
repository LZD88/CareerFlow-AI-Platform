package cn.yq.modules.studentportal.domain.vo.res.aichat;

import lombok.Data;

/**
 * Python Agent 返回的会话摘要结果。
 */
@Data
public class StudentChatPythonSummarizeRes {

    private Boolean ok;
    private String summary;
    private Integer summaryTokenCount;
    private Long compressedUntilMessageId;
}
