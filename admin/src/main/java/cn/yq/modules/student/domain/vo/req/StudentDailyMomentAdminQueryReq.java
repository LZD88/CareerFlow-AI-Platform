package cn.yq.modules.student.domain.vo.req;

import lombok.Data;

@Data
public class StudentDailyMomentAdminQueryReq {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String studentCode;

    private String studentName;

    private String title;
}
