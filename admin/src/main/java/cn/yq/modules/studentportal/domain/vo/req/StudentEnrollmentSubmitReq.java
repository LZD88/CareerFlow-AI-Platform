package cn.yq.modules.studentportal.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Date;

@Data
public class StudentEnrollmentSubmitReq {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入正确的11位手机号")
    private String phone;

    private String password;

    @NotNull(message = "毕业届数不能为空")
    @Min(value = 1950, message = "毕业年份不能早于1950年")
    @Max(value = 2100, message = "毕业年份不能晚于2100年")
    private Integer graduationSession;

    @NotBlank(message = "学校不能为空")
    @Size(max = 128, message = "毕业院校最多128个字符")
    private String school;

    @NotBlank(message = "学历不能为空")
    @Size(max = 64, message = "学历最多64个字符")
    private String education;

    private Long classId;

    @NotNull(message = "加入时间不能为空")
    private Date joinTime;
}
