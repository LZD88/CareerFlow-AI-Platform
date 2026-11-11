package cn.yq.modules.auth.service;

import cn.yq.modules.auth.domain.vo.res.FeishuUserInfo;

public interface FeishuAuthService {

    FeishuUserInfo getUserInfoByCode(String code);
}
