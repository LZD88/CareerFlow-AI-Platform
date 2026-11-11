package cn.yq.modules.teaching.domain.vo.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignStageTeacherReq {

    @Valid
    @NotEmpty(message = "阶段老师分配不能为空")
    private List<StageTeacherAssignItem> items;

    @Data
    public static class StageTeacherAssignItem {
        private String stageName;
        private Long teacherId;
    }
}
