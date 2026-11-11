package cn.yq.modules.payment.service;

public interface PaymentDelayCloseTxService {

    void closeIfStillWaiting(String paymentOrderNo);
}
