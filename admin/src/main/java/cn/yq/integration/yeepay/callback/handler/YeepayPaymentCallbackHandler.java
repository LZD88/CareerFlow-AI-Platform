package cn.yq.integration.yeepay.callback.handler;

import cn.yq.integration.yeepay.callback.service.YeepayPaymentCallbackService;
import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class YeepayPaymentCallbackHandler extends YeepayBaseCallbackHandler {

    private static final String CALLBACK_TYPE = "/paySuccess";

    private final YeepayPaymentCallbackService yeepayPaymentCallbackService;

    @Override
    public String getType() {
        return CALLBACK_TYPE;
    }

    @Override
    public void handle(YopCallback callback) {
        log.info("dispatch yeepay callback, type={}, id={}, validSign={}",
                callback.getType(), callback.getId(), callback.isValidSign());
        yeepayPaymentCallbackService.handle(callback);
    }
}
