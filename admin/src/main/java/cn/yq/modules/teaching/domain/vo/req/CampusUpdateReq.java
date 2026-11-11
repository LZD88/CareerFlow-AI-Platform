package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CampusUpdateReq {

    @NotBlank(message = "校区名称不能为空")
    private String campusName;

    @NotNull(message = "负责人不能为空")
    private Long principalUserId;

    @NotBlank(message = "校区地址不能为空")
    private String address;

    private String contactPhone;

    private String remark;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
