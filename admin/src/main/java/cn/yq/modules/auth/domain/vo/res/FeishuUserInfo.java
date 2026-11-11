package cn.yq.modules.auth.domain.vo.res;

import lombok.Data;

@Data
public class FeishuUserInfo {

    private String accessToken;

    private String openId;

    private String unionId;

    private String userId;

    private String name;

    private String email;
}
