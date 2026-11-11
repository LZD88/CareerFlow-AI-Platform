package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.util.Date;

@Data
public class StudentEnrollmentDraftRes {

    private Long orderId;

    private String orderNo;

    private String studentCode;

    private String studentName;

    private String phone;

    private Long productId;

    private String productName;

    private String studyMode;

    private Long classId;

    private Integer classTerm;

    private Date joinTime;
}
