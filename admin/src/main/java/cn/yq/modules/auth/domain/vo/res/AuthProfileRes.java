package cn.yq.modules.auth.domain.vo.res;

import lombok.Data;

import java.util.List;

@Data
public class AuthProfileRes {

    private Long userId;

    private String username;

    private String nickname;

    private String unionId;

    private Boolean feishuBound;

    private List<String> roleCodes;

    private List<String> permissionCodes;
}
