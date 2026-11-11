package cn.yq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class PaymentDelayTaskConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService paymentDelayTaskExecutor() {
        AtomicInteger threadIndex = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("payment-delay-task-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newScheduledThreadPool(2, threadFactory);
    }
}
