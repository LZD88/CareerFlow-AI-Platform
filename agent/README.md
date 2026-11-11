# CareerFlow · Python AI 服务

Python 3.11 + FastAPI 服务，提供学员问答、RAG、知识入库、数据分析、面试复盘、运营内容生成与日志诊断。Java 负责业务身份与数据，Python 负责模型编排和检索；流式问答通过 NDJSON 事件与 Java SSE 对接。

```bash
uv sync --extra dev --frozen
# 复制 .env.example 为 .env，并按需填入服务配置
uv run uvicorn app.main:app --host 127.0.0.1 --port 9000
uv run --extra dev pytest -q
```

健康检查：GET /health。OpenAPI：/docs。基础接口可启动，真实 AI 请求需要有效模型配置。RAG 依赖 Milvus、embedding 和 rerank；语音能力需要相应服务或可选 local-asr 依赖。

代码入口：

- [对话与摘要](app/services/student_chat/student_chat_service.py)
- [检索路由](app/services/student_chat/rag_router_service.py)
- [Milvus](app/services/knowledge_base/milvus_service.py)
- [SQL 规则校验](app/services/data_analysis/sql_guard.py)
- [配置](app/config/settings.py)

实际能力及外部验证范围见 [项目 README](../README.md)，启动前置条件见 [运行指南](../docs/getting-started.md)。不要把真实 Key 提交到仓库。
