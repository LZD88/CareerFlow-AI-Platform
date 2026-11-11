package cn.yq.modules.payment.service;

public interface PaymentDelayCloseService {

    void scheduleCloseIfStillWaiting(String paymentOrderNo);
}
