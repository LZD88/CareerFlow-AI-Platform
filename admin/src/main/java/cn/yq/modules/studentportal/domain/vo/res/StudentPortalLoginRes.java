package cn.yq.modules.studentportal.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class StudentPortalLoginRes {

    private String action;

    private Long studentId;

    private String studentCode;

    private String studentName;

    private String token;

    private Long expiresIn;

    private String signSecret;

    private Long signSecretExpiresIn;

    private String scope;

    private List<String> permissions;

    private StudentPaymentPreviewRes paymentPreview;

    private StudentEnrollmentDraftRes enrollmentDraft;
}
