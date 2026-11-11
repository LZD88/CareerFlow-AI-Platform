package cn.yq.modules.auth.domain.vo.res;

import lombok.Data;

import java.util.List;

/**
 * 登录返回结果。
 */
@Data
public class LoginRes {

    private String token;
    private Long expiresIn;
    private Long userId;
    private String username;
    private String nickname;
    private String unionId;
    private String signSecret;
    private Long signSecretExpiresIn;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}
