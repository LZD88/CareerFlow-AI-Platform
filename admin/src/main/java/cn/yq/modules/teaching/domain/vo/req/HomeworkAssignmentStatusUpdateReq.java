package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomeworkAssignmentStatusUpdateReq {

    @NotBlank(message = "状态不能为空")
    private String status;
}
