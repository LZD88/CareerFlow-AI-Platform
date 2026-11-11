package cn.yq.modules.payment.service.impl;

import cn.yq.modules.payment.service.PaymentDelayCloseService;
import cn.yq.modules.payment.service.PaymentDelayCloseTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDelayCloseServiceImpl implements PaymentDelayCloseService {

    private static final long CLOSE_DELAY_MINUTES = 6L;

    @Qualifier("paymentDelayTaskExecutor")
    private final ScheduledExecutorService paymentDelayTaskExecutor;
    private final PaymentDelayCloseTxService paymentDelayCloseTxService;

    @Override
    public void scheduleCloseIfStillWaiting(String paymentOrderNo) {
        paymentDelayTaskExecutor.schedule(() -> {
            try {
                paymentDelayCloseTxService.closeIfStillWaiting(paymentOrderNo);
            } catch (Exception ex) {
                log.error("payment delay close task failed, paymentOrderNo={}", paymentOrderNo, ex);
            }
        }, CLOSE_DELAY_MINUTES, TimeUnit.MINUTES);
    }
}
