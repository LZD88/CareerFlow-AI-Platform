package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StuPermissionTemplateCreateReq {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    private String studyMode;

    private Long productId;

    private Long classId;

    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
