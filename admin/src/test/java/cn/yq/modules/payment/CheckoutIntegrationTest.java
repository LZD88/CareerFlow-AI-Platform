package cn.yq.modules.payment;

import cn.yq.common.exception.BusinessException;
import cn.yq.config.YeepayProperties;
import cn.yq.config.YeepayDynamicProperties;
import cn.yq.integration.yeepay.callback.service.impl.YeepayPaymentCallbackTxServiceImpl;
import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderReq;
import cn.yq.integration.yeepay.domain.YeepayUnifiedOrderRes;
import cn.yq.integration.yeepay.service.YeepayCashierService;
import cn.yq.integration.yeepay.service.YeepayReadiness;
import cn.yq.modules.payment.domain.entity.PaymentOrderEntity;
import cn.yq.modules.payment.mapper.PaymentOrderMapper;
import cn.yq.modules.payment.service.OrderCheckoutService;
import cn.yq.modules.payment.service.PaymentChannelOrderService;
import cn.yq.modules.sales.mapper.OrderMapper;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeepay.yop.sdk.service.common.callback.YopCallback;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真实 MyBatis SQL + H2 事务；仅渠道 SDK 调用与验签结果使用测试替身，不发起扣款。 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CheckoutIntegrationTest.Config.class)
class CheckoutIntegrationTest {
    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:payment_checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", ""); }
        @Bean DataSourceTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean TransactionTemplate transactions(DataSourceTransactionManager tm) { return new TransactionTemplate(tm); }
        @Bean SqlSessionFactory factory(DataSource ds) throws Exception {
            var f = new SqlSessionFactoryBean(); f.setDataSource(ds);
            f.setMapperLocations(new ClassPathResource("mapper/PaymentOrderMapper.xml"), new ClassPathResource("mapper/OrderMapper.xml"));
            return f.getObject();
        }
        @Bean SqlSessionTemplate sql(SqlSessionFactory f) { return new SqlSessionTemplate(f); }
        @Bean PaymentOrderMapper payments(SqlSessionTemplate sql) { return sql.getMapper(PaymentOrderMapper.class); }
        @Bean OrderMapper orders(SqlSessionTemplate sql) { return sql.getMapper(OrderMapper.class); }
        @Bean YeepayProperties properties() { var p = new YeepayProperties(); p.setMerchantNo("TEST_MERCHANT"); return p; }
        @Bean YeepayDynamicProperties dynamic() { return mock(YeepayDynamicProperties.class); }
        @Bean YeepayReadiness readiness() { return mock(YeepayReadiness.class); }
        @Bean YeepayCashierService cashier() { return mock(YeepayCashierService.class); }
        @Bean PaymentChannelOrderService channels() { return mock(PaymentChannelOrderService.class); }
        @Bean RedissonClient redisson() { return mock(RedissonClient.class); }
        @Bean OrderCheckoutService checkout(OrderMapper o, PaymentOrderMapper p, YeepayProperties props, YeepayDynamicProperties d, YeepayReadiness r, YeepayCashierService c, RedissonClient redis, TransactionTemplate tx) { return new OrderCheckoutService(o,p,props,d,r,c,redis,tx); }
        @Bean YeepayPaymentCallbackTxServiceImpl callbacks(PaymentOrderMapper p, PaymentChannelOrderService c, OrderMapper o, YeepayProperties props) { return new YeepayPaymentCallbackTxServiceImpl(p,c,o,props); }
    }

    @Autowired DataSource ds;
    @Autowired OrderCheckoutService checkout;
    @Autowired PaymentOrderMapper payments;
    @Autowired OrderMapper orders;
    @Autowired YeepayCashierService cashier;
    @Autowired YeepayReadiness readiness;
    @Autowired YeepayDynamicProperties dynamic;
    @Autowired RedissonClient redis;
    @Autowired PaymentChannelOrderService channels;
    @Autowired YeepayPaymentCallbackTxServiceImpl callbacks;
    JdbcTemplate db;

    @BeforeEach void setup() throws Exception {
        reset(cashier, readiness, dynamic, redis, channels);
        when(dynamic.getExpireMinutes()).thenReturn(5);
        when(readiness.missing()).thenReturn(List.of());
        RLock lock = mock(RLock.class);
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(redis.getLock(anyString())).thenReturn(lock);
        when(cashier.unifiedOrder(any())).thenReturn(YeepayUnifiedOrderRes.builder().uniqueOrderNo("CHANNEL_1").cashierUrl("https://cashier.example.test/pay").build());
        db = new JdbcTemplate(ds);
        db.execute("drop all objects");
        db.execute("create table sale_product(id bigint primary key, product_name varchar(100), study_mode varchar(20))");
        db.execute("create table edu_class(id bigint primary key, class_term int)");
        db.execute("create table edu_student(id bigint primary key, student_code varchar(64), product_id bigint, phone varchar(32), class_id bigint)");
        db.execute("create table sale_order(id bigint primary key, order_no varchar(64), student_name varchar(64), phone varchar(32), product_id bigint, class_id bigint, student_code varchar(64), product_amount decimal(12,2), discount_amount decimal(12,2), receivable_amount decimal(12,2), paid_amount decimal(12,2), order_status varchar(32), created_at timestamp, updated_at timestamp)");
        db.execute("create table sale_payment_order(id bigint auto_increment primary key, payment_order_no varchar(64) unique, order_no varchar(64), student_code varchar(64), student_name varchar(64), phone varchar(32), product_name varchar(128), payment_amount decimal(12,2), payment_channel varchar(32), payment_status varchar(32), unique_order_no varchar(64), last_error_msg varchar(255), cashier_url clob, expires_at timestamp, merchant_no varchar(64), created_at timestamp, updated_at timestamp)");
        db.execute("insert into sale_product values(2,'测试课程','ONLINE')");
        db.execute("insert into sale_order values(1,'ORDER_1','测试学员','13800000000',2,null,'STU_1',1000,0,1000,300,'PARTIAL_PAID',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
    }

    @Test void chargesOnlyBalanceAndReusesPersistedLink() {
        var first = checkout.create(1L);
        var second = checkout.create(1L);
        assertEquals(0, first.getPaymentAmount().compareTo(new BigDecimal("700.00")));
        assertEquals(first.getPaymentOrderNo(), second.getPaymentOrderNo());
        assertEquals("https://cashier.example.test/pay", second.getCashierUrl());
        assertTrue(first.getPaymentOrderNo().matches("ZF[a-f0-9]{32}"));
        assertEquals(1, db.queryForObject("select count(*) from sale_payment_order", Integer.class));
        var captor = ArgumentCaptor.forClass(YeepayUnifiedOrderReq.class);
        verify(cashier).unifiedOrder(captor.capture());
        assertEquals(0, captor.getValue().getOrderAmount().compareTo(new BigDecimal("700")));
        assertNotNull(captor.getValue().getExpiresAt());
        assertEquals(1, orders.countOpenPayments("ORDER_1"));
    }

    @Test void refusesPaidClosedAndMissingConfigurationWithoutCreatingPayment() {
        doThrow(new BusinessException(503,"未配置")).when(readiness).requireReady();
        assertThrows(BusinessException.class, () -> checkout.create(1L));
        reset(readiness);
        for (String status : List.of("PAID","CLOSED")) {
            db.update("update sale_order set order_status=? where id=1", status);
            assertThrows(BusinessException.class, () -> checkout.create(1L));
        }
        assertEquals(0, db.queryForObject("select count(*) from sale_payment_order", Integer.class));
        verifyNoInteractions(cashier);
    }

    @Test void leavesUncertainAttemptAndBlocksDuplicateChargeAfterTimeout() {
        when(cashier.unifiedOrder(any())).thenThrow(new RuntimeException("timeout"));
        assertThrows(BusinessException.class, () -> checkout.create(1L));
        assertThrows(BusinessException.class, () -> checkout.create(1L));
        assertEquals("PROCESSING", checkout.preview(1L).getPaymentStatus());
        assertEquals(1, db.queryForObject("select count(*) from sale_payment_order", Integer.class));
        verify(cashier).unifiedOrder(any());
    }

    @Test void expiredCashierBlocksNewAttemptUntilReconciled() {
        checkout.create(1L);
        db.update("update sale_payment_order set expires_at=?", new Date(System.currentTimeMillis()-10000));
        assertTrue(checkout.preview(1L).isExpired());
        assertNull(checkout.preview(1L).getCashierUrl());
        assertThrows(BusinessException.class, () -> checkout.create(1L));
        verify(cashier).unifiedOrder(any());
    }

    @Test void duplicateSuccessAndLateFailureNeverDoubleCreditOrDowngrade() {
        var view = checkout.create(1L);
        notify(view.getPaymentOrderNo(), "SUCCESS", "700.00", "TEST_MERCHANT", true);
        notify(view.getPaymentOrderNo(), "SUCCESS", "700.00", "TEST_MERCHANT", true);
        notify(view.getPaymentOrderNo(), "FAIL", "700.00", "TEST_MERCHANT", true);
        assertEquals("PAY_SUCCESS", checkout.preview(1L).getPaymentStatus());
        assertEquals("PAID", orders.selectById(1L).getOrderStatus());
        assertEquals(0, orders.selectById(1L).getPaidAmount().compareTo(new BigDecimal("1000")));
        verify(channels).saveOrUpdate(any());
    }

    @Test void concurrentDuplicateCallbacksCreditExactlyOnce() throws Exception {
        var view = checkout.create(1L);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            Callable<Void> call = () -> { start.await(); notify(view.getPaymentOrderNo(),"SUCCESS","700","TEST_MERCHANT",true); return null; };
            var a = pool.submit(call); var b = pool.submit(call); start.countDown();
            a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
            assertEquals(0, orders.selectById(1L).getPaidAmount().compareTo(new BigDecimal("1000")));
            verify(channels).saveOrUpdate(any());
        } finally { pool.shutdownNow(); }
    }

    @Test void invalidSignatureMerchantAndAmountLeaveBothOrdersUnchanged() {
        var view = checkout.create(1L);
        assertThrows(BusinessException.class, () -> notify(view.getPaymentOrderNo(),"SUCCESS","700","TEST_MERCHANT",false));
        assertThrows(BusinessException.class, () -> notify(view.getPaymentOrderNo(),"SUCCESS","700","OTHER",true));
        assertThrows(BusinessException.class, () -> notify(view.getPaymentOrderNo(),"SUCCESS","1","TEST_MERCHANT",true));
        assertEquals("UNIFIED_ORDER_CREATED", checkout.preview(1L).getPaymentStatus());
        assertEquals(0, orders.selectById(1L).getPaidAmount().compareTo(new BigDecimal("300")));
        verifyNoInteractions(channels);
    }

    @Test void channelPersistenceFailureRollsBackPaymentAndEnrollmentOrder() {
        var view = checkout.create(1L);
        doThrow(new RuntimeException("database failure")).when(channels).saveOrUpdate(any());
        assertThrows(RuntimeException.class, () -> notify(view.getPaymentOrderNo(),"SUCCESS","700","TEST_MERCHANT",true));
        assertEquals("UNIFIED_ORDER_CREATED", checkout.preview(1L).getPaymentStatus());
        assertEquals(0, orders.selectById(1L).getPaidAmount().compareTo(new BigDecimal("300")));
    }

    @Test void earlySuccessCallbackCannotBeOverwrittenByLateUnifiedOrderResponse() {
        when(cashier.unifiedOrder(any())).thenAnswer(invocation -> {
            YeepayUnifiedOrderReq request = invocation.getArgument(0);
            notify(request.getPaymentOrderNo(),"SUCCESS","700","TEST_MERCHANT",true);
            return YeepayUnifiedOrderRes.builder().uniqueOrderNo("CHANNEL_1").cashierUrl("https://cashier.example.test/pay").build();
        });
        assertEquals("PAY_SUCCESS", checkout.create(1L).getPaymentStatus());
        assertEquals("PAID", orders.selectById(1L).getOrderStatus());
    }

    @Test void orderUpdateFailureRollsBackPreviouslyWrittenPaymentSuccess() {
        var view = checkout.create(1L);
        db.execute("alter table sale_order add constraint paid_limit check (paid_amount <= 500)");
        assertThrows(RuntimeException.class, () -> notify(view.getPaymentOrderNo(),"SUCCESS","700","TEST_MERCHANT",true));
        assertEquals("UNIFIED_ORDER_CREATED", checkout.preview(1L).getPaymentStatus());
        assertEquals(0, orders.selectById(1L).getPaidAmount().compareTo(new BigDecimal("300")));
    }

    @Test void closedOrderStillRecordsLateMoneyWithoutReopening() {
        var view = checkout.create(1L);
        db.update("update sale_order set order_status='CLOSED' where id=1");
        notify(view.getPaymentOrderNo(),"SUCCESS","700","TEST_MERCHANT",true);
        assertEquals("CLOSED", orders.selectById(1L).getOrderStatus());
        assertEquals(0, orders.selectById(1L).getPaidAmount().compareTo(new BigDecimal("1000")));
    }

    @Test void publicStatusDoesNotDiscloseCashierPiiOrLoginCredentials() throws Exception {
        var view = checkout.create(1L);
        String json = new ObjectMapper().writeValueAsString(checkout.publicStatus(1L,view.getPaymentOrderNo()));
        assertFalse(json.contains("13800000000"));
        assertFalse(json.contains("测试学员"));
        assertFalse(json.contains("cashier.example"));
        assertFalse(json.contains("accessToken"));
        assertFalse(json.contains("signSecret"));
        assertThrows(BusinessException.class, () -> checkout.publicStatus(999L,view.getPaymentOrderNo()));
        assertThrows(BusinessException.class, () -> checkout.publicStatus(1L,"missing"));
    }

    private void notify(String no, String status, String amount, String merchant, boolean valid) {
        var data = new JSONObject();
        data.put("orderId", no); data.put("uniqueOrderNo","CHANNEL_1"); data.put("merchantNo",merchant);
        data.put("status",status); data.put("orderAmount",amount); data.put("payAmount",amount);
        var callback = mock(YopCallback.class);
        when(callback.isValidSign()).thenReturn(valid);
        when(callback.getBizData()).thenReturn(data.toJSONString());
        callbacks.persistCallbackResult(callback,data.toJSONString(),"CHANNEL_1",status,payments.selectByPaymentOrderNo(no));
    }
}
