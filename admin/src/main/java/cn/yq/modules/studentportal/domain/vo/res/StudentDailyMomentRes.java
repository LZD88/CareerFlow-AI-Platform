package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class StudentDailyMomentRes {

    private Long id;

    private String title;

    private String contentText;

    private List<String> fileUrls;

    private Date recordTime;

    private Date createTime;
}
