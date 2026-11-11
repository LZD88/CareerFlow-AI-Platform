package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClassDutySaveReq {

    @NotBlank(message = "值班日期不能为空")
    private String dutyDate;

    @NotNull(message = "值班老师不能为空")
    private Long teacherId;

    private String remark;
}
