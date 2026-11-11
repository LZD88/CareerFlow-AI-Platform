package cn.yq.modules.studentportal.domain.vo.req.mockinterview;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MockInterviewCreateSessionReq {

    @Size(max = 80, message = "面试标题不能超过80个字符")
    private String title;

    @Size(max = 100, message = "面试岗位不能超过100个字符")
    private String interviewRole;
}
