package cn.yq.modules.student.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class StudentCreateReq {

    @NotBlank(message = "学号不能为空")
    private String studentCode;

    @NotBlank(message = "姓名不能为空")
    private String studentName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String password;

    @NotNull(message = "毕业届数不能为空")
    private Integer graduationSession;

    @NotBlank(message = "学校不能为空")
    private String school;

    @NotBlank(message = "学历不能为空")
    private String education;

    @NotBlank(message = "学习方式不能为空")
    private String studyMode;

    private Long classId;

    @NotNull(message = "产品不能为空")
    private Long productId;

    @NotNull(message = "加入时间不能为空")
    private Date joinTime;
}
