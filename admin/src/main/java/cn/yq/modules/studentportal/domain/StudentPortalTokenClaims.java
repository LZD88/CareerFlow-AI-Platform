package cn.yq.modules.studentportal.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentPortalTokenClaims {

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String scope;

    private Long orderId;
}
