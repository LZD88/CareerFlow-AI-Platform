package cn.yq.integration.yeepay.callback.handler;

import com.yeepay.yop.sdk.service.common.callback.handler.YopCallbackHandler;
import com.yeepay.yop.sdk.service.common.callback.handler.YopCallbackHandlerFactory;
import jakarta.annotation.PostConstruct;

public abstract class YeepayBaseCallbackHandler implements YopCallbackHandler {

    @PostConstruct
    public void init() {
        YopCallbackHandlerFactory.register(getType(), this);
    }
}
