package cn.yq.modules.system.domain.entity;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

@Data
public class SysUserEntity {

    /** 用户ID */
    private Long id;
    /** 登录用户名 */
    private String username;
    /** 登录密码，当前为测试阶段明文存储 */
    @JsonIgnore
    private String password;
    /** 展示昵称 */
    private String nickname;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 飞书 union_id */
    private String unionId;
    /** 状态，1启用，0禁用 */
    private Integer status;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
}
