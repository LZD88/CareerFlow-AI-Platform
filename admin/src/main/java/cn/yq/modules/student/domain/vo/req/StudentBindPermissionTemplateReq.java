package cn.yq.modules.student.domain.vo.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class StudentBindPermissionTemplateReq {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    private Date effectiveStartTime;

    private Date effectiveEndTime;

    private Integer status = 1;
}
