# CareerFlow · Java 业务中心

基于 Java 17、Spring Boot 3.2、MyBatis 和 Redis/Redisson，承载职业培训与就业服务平台的认证权限、课程商品、报名收款、入班建档、教学履约及 AI 业务接入。

重点代码：

- [支付收银台](src/main/java/cn/yq/modules/payment/service/OrderCheckoutService.java)
- [支付回调事务](src/main/java/cn/yq/integration/yeepay/callback/service/impl/YeepayPaymentCallbackTxServiceImpl.java)
- [订单入班](src/main/java/cn/yq/modules/sales/service/OrderEnrollmentService.java)
- [学员问答 SSE](src/main/java/cn/yq/modules/studentportal/biz/impl/StudentPortalAiChatBizImpl.java)
- [集成与单元测试](src/test/java/cn/yq)

```bash
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认监听 8081，context-path 为 /yq-admin。先按 [启动指南](../docs/getting-started.md) 初始化本地库与 Redis。外部支付、飞书、对象存储通过环境变量配置，实际商户 SDK 配置应放在仓库外；示例位于 [docs/yop_sdk_config.example.json](docs/yop_sdk_config.example.json)。

[项目介绍与实现边界](../README.md) · [易宝收款说明](../web/docs/payment-integration.md)
