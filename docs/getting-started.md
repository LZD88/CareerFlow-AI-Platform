# 本地启动指南

本仓库包含 Java、React 和 Python 三个模块。基础业务依赖 MySQL 与 Redis；模型、Milvus、支付、飞书和 TOS 按需配置。

## 1. 安装工具

- JDK 17 或更高版本、Maven 3.9+
- Node.js 20+（含 npm）
- Python 3.11 与 [uv](https://docs.astral.sh/uv/)
- MySQL 8 服务及 mysql、mysqldump 命令行工具
- Redis 服务；也可使用 Docker Compose 启动基础设施

## 2. 启动数据库和缓存

已有本地 MySQL/Redis 时直接使用；确保端口和账号与下表一致。新环境可在仓库根目录执行：

```bash
docker compose up -d
```

| 组件 | 本地默认值 |
| --- | --- |
| MySQL | 127.0.0.1:3306，root 密码 123456 |
| 业务库 | training_career_local |
| 业务用户 | training_local / training-local-db |
| Redis | 127.0.0.1:6380，密码 training-local-redis，DB 4 |

Compose 的端口仅绑定回环地址，凭据为公开的本地开发样例。若端口已有服务占用，使用现有实例或同步修改 Compose 和服务配置。

## 3. 初始化数据库

在全新本地开发库运行，脚本会创建表、权限和管理员，并应用业务 SQL：

```bash
node admin/scripts/init-local.cjs
```

脚本固定连接 127.0.0.1:3306 的 training_career_local，不读取远程数据库配置。默认调用 PATH 中的 mysql。工具未加入 PATH 时设置 LOCAL_MYSQL_BIN；root 密码不同则设置 LOCAL_MYSQL_ROOT_PASSWORD。

PowerShell 示例：

```powershell
$env:LOCAL_MYSQL_BIN = 'C:/mysql/bin/mysql.exe'
$env:LOCAL_MYSQL_ROOT_PASSWORD = '自己的本地 root 密码'
node admin/scripts/init-local.cjs
```

可选生成演示数据：

```bash
node admin/scripts/seed-demo.cjs
```

脚本先使用 mysqldump 备份到 admin/.local-run，再插入带 DEMO 标记的本地数据。重复执行按业务键复用既有数据；请勿将该脚本用于生产数据库。备份工具不在 PATH 时设置 LOCAL_MYSQLDUMP_BIN。

## 4. 安装依赖与配置

在 agent 目录：

```bash
uv sync --extra dev --frozen
```

将 agent/.env.example 复制为 agent/.env，按需配置 QWEN_API_KEY 等。未配置模型时可运行基础业务和非模型页面，模型调用会返回配置错误。RAG 还需要 Milvus 与 embedding/rerank 服务；语音复盘和实时面试需相应语音服务。可选本地 ASR 使用 `uv sync --extra dev --extra local-asr` 安装，模型下载和资源消耗由运行环境决定。

在 web 目录：

```bash
npm ci
```

首次启动前，将 web/.env.example 复制为 web/.env.local。示例已配置 API 地址 http://127.0.0.1:8081/yq-admin 及签名前缀 /yq-admin；请求签名路径前缀必须与 Java context-path 保持一致。

Java 默认使用 local profile。数据库和 Redis 可通过环境变量覆盖：

| 变量 | 用途 |
| --- | --- |
| LOCAL_DB_URL / LOCAL_DB_USER / LOCAL_DB_PASSWORD | Java 数据源 |
| LOCAL_REDIS_HOST / LOCAL_REDIS_PORT / LOCAL_REDIS_PASSWORD | 缓存与锁 |
| YQ_PYTHON_AGENT_BASE_URL | Python HTTP 地址，默认 http://127.0.0.1:9000 |
| YQ_FEISHU_APP_ID / YQ_FEISHU_APP_SECRET | 可选飞书登录 |
| TOS_BUCKET_NAME / TOS_ACCESS_KEY / TOS_SECRET_KEY | 可选对象存储 |
| YQ_YEEPAY_* | 可选易宝商户配置，见支付文档 |

真实配置放在仓库外或已忽略的 .env 中。不要将服务端 Key 放入 VITE_* 变量。Java 与 Python 的实时面试 token secret 应配置为相同的自定义值。

## 5. 分别启动三项服务

终端一，工作目录 agent：

```bash
uv run uvicorn app.main:app --host 127.0.0.1 --port 9000
```

终端二，工作目录 admin：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

PowerShell 可将 Maven 参数加引号：

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=local'
```

终端三，工作目录 web：

```bash
npm run dev -- --host 127.0.0.1
```

访问 http://127.0.0.1:5173，使用 admin / password 登录。初始化 demo 数据后也可使用 demo_ops / Demo123456，讲师账号为 demo_teacher_1 至 demo_teacher_8。独立学员前端未包含在仓库中。

Windows 可选脚本 web/scripts/start-local.ps1 会启动本地 Redis 进程和三项服务；要求相关工具可执行，且不能与 Compose 中已启动的同端口 Redis 重复启动。优先使用上述分终端方式，便于查看日志和调整配置。

## 6. 验证

```bash
# admin
mvn test

# agent
uv run --extra dev pytest -q

# web
npm run build
```

支付浏览器验证脚本需要额外安装 Playwright：

```bash
# web
npm install --no-save --package-lock=false playwright
npx playwright install chromium
node scripts/verify-payment-local.cjs
```

运行前需启动本地三服务、加载演示数据，并确保 demo_ops / demo_teacher_1 账号存在。脚本会在本地测试库创建带测试标记的订单；报告默认写入 web/.local-run/payment。部分 UI 场景使用拦截响应，脚本不发起真实渠道支付。

## 7. 外部接入与部署

- [易宝收款配置与验证边界](../web/docs/payment-integration.md)
- 飞书登录需前后端使用同一个应用，并登记对应回调地址。
- 云存储和语音服务需配置有效账号；演示复盘不依赖真实录音识别。
- 前端部署可参考 web/.env.production.example，构建后由 Nginx 提供静态资源；API 代理路径须与请求签名一致，SSE 路径需关闭代理缓冲。
- Java 和 Python 分别运行，外部域名仅暴露必要入口。仓库中的 Compose 只用于本地 MySQL/Redis，不是完整生产部署方案。
