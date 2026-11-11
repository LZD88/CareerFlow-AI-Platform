package cn.yq.modules.studentportal.domain;

import lombok.Data;

@Data
public class StudentPortalAccessContext {

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String scope;

    private Long orderId;
}
