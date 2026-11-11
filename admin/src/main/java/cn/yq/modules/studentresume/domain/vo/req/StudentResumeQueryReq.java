package cn.yq.modules.studentresume.domain.vo.req;

import lombok.Data;

@Data
public class StudentResumeQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String targetRole;

    private Boolean defaultResume;
}
