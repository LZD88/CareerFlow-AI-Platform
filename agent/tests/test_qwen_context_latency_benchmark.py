import os
import time
from dataclasses import dataclass

import pytest
from langchain_openai import ChatOpenAI

from app.config.settings import get_settings


def _estimate_tokens(text: str) -> int:
    """
    粗略估算 token 数。

    这里只用于对比不同上下文规模时的延迟趋势，不追求精确 tokenizer 结果。
    - 中文/日文/韩文按 1 字约 1 token 估算；
    - ASCII 文本按 4 字符约 1 token 估算。
    """
    cjk_count = sum(1 for ch in text if "\u4e00" <= ch <= "\u9fff")
    ascii_count = len(text) - cjk_count
    return cjk_count + max(ascii_count // 4, 0)


def _parse_context_sizes() -> list[int]:
    raw = os.getenv("QWEN_BENCHMARK_CONTEXT_CHARS", "").strip()
    if not raw:
        return [1_000, 3_000, 6_000, 12_000, 24_000, 48_000]
    result: list[int] = []
    for item in raw.split(","):
        value = item.strip()
        if not value:
            continue
        result.append(int(value))
    return result or [1_000, 3_000, 6_000, 12_000, 24_000, 48_000]


def _parse_bool_env(name: str, default: bool) -> bool:
    raw = os.getenv(name, "").strip().lower()
    if not raw:
        return default
    return raw in {"1", "true", "yes", "y", "on"}


def _build_schema_blob(target_chars: int) -> str:
    unit = """
表名: sale_order
DDL:
CREATE TABLE IF NOT EXISTS sale_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    student_name VARCHAR(64) NOT NULL COMMENT '学员姓名',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    class_id BIGINT NULL COMMENT '班级ID',
    product_amount DECIMAL(10,2) NOT NULL COMMENT '产品金额',
    receivable_amount DECIMAL(10,2) NOT NULL COMMENT '应收金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '已付金额',
    order_status VARCHAR(32) NOT NULL COMMENT '订单状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_product_id (product_id),
    KEY idx_class_id (class_id),
    KEY idx_order_status (order_status)
) COMMENT='订单表';

表名: sale_payment_order
DDL:
CREATE TABLE IF NOT EXISTS sale_payment_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '支付流水ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    payment_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    payment_status VARCHAR(32) NOT NULL COMMENT '支付状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_order_no (order_no),
    KEY idx_payment_status (payment_status)
) COMMENT='支付流水表';

表名: edu_class
DDL:
CREATE TABLE IF NOT EXISTS edu_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '班级ID',
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    campus_id BIGINT NOT NULL COMMENT '校区ID'
) COMMENT='班级表';

表名: edu_campus
DDL:
CREATE TABLE IF NOT EXISTS edu_campus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '校区ID',
    campus_name VARCHAR(100) NOT NULL COMMENT '校区名称'
) COMMENT='校区表';
""".strip()
    chunks: list[str] = []
    total = 0
    while total < target_chars:
        chunks.append(unit)
        total += len(unit) + 2
    return "\n\n".join(chunks)[:target_chars]


def _build_prompt(context_chars: int) -> str:
    schema_blob = _build_schema_blob(context_chars)
    return f"""你是 MySQL 数据分析 SQL 生成助手。
请根据用户问题和提供的 DDL 生成一条可执行的只读 SQL。

规则：
1. 只能生成 SELECT 或 WITH 查询。
2. 只能使用候选表和提供的 DDL。
3. SQL 必须适配 MySQL。
4. 只返回 JSON。

用户问题：
统计 2026-04-29 每个校区支付成功订单金额，支付成功状态为 PAY_SUCCESS。

候选表：
["sale_order", "sale_payment_order", "edu_class", "edu_campus"]

业务指标口径：
指标名称：支付成功订单金额
计算口径：SUM(sale_payment_order.payment_amount)
默认过滤：sale_payment_order.payment_status = 'PAY_SUCCESS'
默认时间字段：sale_payment_order.created_at
关联链路：sale_payment_order.order_no -> sale_order.order_no -> edu_class -> edu_campus

表结构 DDL：
{schema_blob}
"""


@dataclass
class BenchmarkRow:
    context_chars: int
    estimated_tokens: int
    elapsed_seconds: float
    response_chars: int
    success: bool
    error: str = ""


@pytest.mark.skipif(
    os.getenv("RUN_QWEN_BENCHMARK") != "1",
    reason="仅手动开启时运行真实模型基准测试，避免日常 pytest 触发外部调用。",
)
class TestQwenContextLatencyBenchmark:
    """
    手动基准测试：

    RUN_QWEN_BENCHMARK=1 uv run --extra dev pytest -s tests/test_qwen_context_latency_benchmark.py

    可选环境变量：
    - QWEN_BENCHMARK_MODEL=qwen3.6-plus
    - QWEN_BENCHMARK_CONTEXT_CHARS=1000,3000,6000,12000,24000
    - QWEN_BENCHMARK_MAX_TOKENS=800
    - QWEN_BENCHMARK_ENABLE_THINKING=false
    """

    def test_benchmark_qwen_context_latency(self) -> None:
        settings = get_settings()
        if not settings.qwen_api_key:
            pytest.skip("未配置 QWEN_API_KEY，跳过真实模型延迟测试")

        model_name = os.getenv("QWEN_BENCHMARK_MODEL", settings.qwen_model).strip() or settings.qwen_model
        max_tokens = int(os.getenv("QWEN_BENCHMARK_MAX_TOKENS", "800"))
        enable_thinking = _parse_bool_env("QWEN_BENCHMARK_ENABLE_THINKING", False)
        context_sizes = _parse_context_sizes()

        llm = ChatOpenAI(
            api_key=settings.qwen_api_key,
            base_url=settings.qwen_base_url,
            model=model_name,
            temperature=0,
            max_tokens=max_tokens,
            extra_body={"enable_thinking": enable_thinking},
        )

        print("")
        print(f"[QWEN BENCHMARK] model={model_name}, max_tokens={max_tokens}, enable_thinking={enable_thinking}")
        print(f"[QWEN BENCHMARK] context_sizes(chars)={context_sizes}")

        results: list[BenchmarkRow] = []
        for context_chars in context_sizes:
            prompt = _build_prompt(context_chars)
            estimated_tokens = _estimate_tokens(prompt)
            started_at = time.perf_counter()
            try:
                response = llm.invoke(prompt)
                elapsed = time.perf_counter() - started_at
                content = response.content if isinstance(response.content, str) else str(response.content)
                row = BenchmarkRow(
                    context_chars=context_chars,
                    estimated_tokens=estimated_tokens,
                    elapsed_seconds=elapsed,
                    response_chars=len(content),
                    success=True,
                )
            except Exception as ex:  # pragma: no cover - 手动基准测试主要看打印结果
                elapsed = time.perf_counter() - started_at
                row = BenchmarkRow(
                    context_chars=context_chars,
                    estimated_tokens=estimated_tokens,
                    elapsed_seconds=elapsed,
                    response_chars=0,
                    success=False,
                    error=str(ex),
                )
            results.append(row)
            print(
                f"[QWEN BENCHMARK] chars={row.context_chars:>6} "
                f"est_tokens={row.estimated_tokens:>6} "
                f"elapsed={row.elapsed_seconds:>7.2f}s "
                f"success={row.success} "
                f"response_chars={row.response_chars:>5} "
                f"error={row.error}"
            )

        successful_rows = [row for row in results if row.success]
        if successful_rows:
            slowest = max(successful_rows, key=lambda item: item.elapsed_seconds)
            print("")
            print(
                "[QWEN BENCHMARK] slowest_success: "
                f"chars={slowest.context_chars}, "
                f"est_tokens={slowest.estimated_tokens}, "
                f"elapsed={slowest.elapsed_seconds:.2f}s"
            )

        assert results, "至少要产出一条基准结果"
