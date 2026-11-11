# CareerFlow · 运营管理端

React 18 + TypeScript + Vite + Ant Design 管理端，提供业务工作台、课程商品、报名订单、收款、教学履约、学员档案、就业辅导与 AI 支撑页面。

```bash
npm ci
npm run dev -- --host 127.0.0.1
npm run build
```

首次启动前复制 .env.example 为 .env.local，配置 Java http://127.0.0.1:8081/yq-admin 及签名前缀。VITE_API_SIGN_PATH_PREFIX 应与后端 context-path 一致。

[完整启动步骤](../docs/getting-started.md) · [支付入口与渠道配置](docs/payment-integration.md) · [项目架构](../README.md)

scripts/start-local.ps1 为可选 Windows 启动器；scripts/verify-payment-local.cjs 验证本地支付 UI/API，需另行安装 Playwright，具体见启动指南。
