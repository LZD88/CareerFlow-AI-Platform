from app.services.knowledge_base.chunk_service import ChunkService


def test_markdown_split_nodes_preserves_header_path() -> None:
    service = ChunkService(chunk_size=256, chunk_overlap=20)

    nodes = service.split_nodes(
        """
# 缴费说明

平台支持一次性缴费和分期缴费。

## Java 线下班

Java 线下班学费为 20000 元，可以联系班主任确认优惠政策。
        """,
        source_type="markdown",
        metadata={"document_id": "payment_001", "title": "缴费说明"},
    )

    assert nodes
    assert any("Java 线下班" in node.get_content() for node in nodes)
    assert any("缴费说明" in str(node.metadata.get("header_path", "")) for node in nodes)


def test_manual_text_split_nodes_keeps_metadata() -> None:
    service = ChunkService(chunk_size=256, chunk_overlap=20)

    nodes = service.split_nodes(
        "HashMap 是 Java 中常用的 Map 实现类。它不是线程安全的。",
        source_type="manual",
        metadata={"document_id": "java_hashmap_001", "title": "HashMap 说明"},
    )

    assert nodes
    assert nodes[0].metadata["document_id"] == "java_hashmap_001"
    assert "HashMap" in nodes[0].get_content()


def test_generated_java_markdown_splits_by_method_sections() -> None:
    service = ChunkService(chunk_size=256, chunk_overlap=20)

    nodes = service.split_nodes(
        """
# AuthServiceImpl

## AuthServiceImpl.login(LoginReq request)

路径: cn.yq.modules.auth.service.impl.AuthServiceImpl
异常: BusinessException(401, "用户名或密码错误")
关键词: AuthServiceImpl, login, 用户名或密码错误

```java
0042 | public LoginRes login(LoginReq request) {
0045 |     throw new BusinessException(401, "用户名或密码错误");
0051 | }
```

## AuthServiceImpl.requireUser(Long id)

路径: cn.yq.modules.auth.service.impl.AuthServiceImpl
异常: BusinessException(404, "用户不存在")
关键词: AuthServiceImpl, requireUser, 用户不存在
        """,
        source_type="generated_java_md",
        metadata={"document_id": "auth_service", "title": "AuthServiceImpl"},
    )

    assert len(nodes) == 2
    assert nodes[0].metadata["chunk_type"] == "java_method"
    assert nodes[0].metadata["method_name"] == "login"
    assert "AuthServiceImpl.login" in nodes[0].get_content()
    assert nodes[1].metadata["method_name"] == "requireUser"


def test_generated_java_markdown_handles_bom_and_indented_headings() -> None:
    service = ChunkService(chunk_size=256, chunk_overlap=20)

    nodes = service.split_nodes(
        "\ufeff# AuthServiceImpl\r\n\r\n"
        "  ## AuthServiceImpl.login(LoginReq request)\r\n"
        "异常: BusinessException(401, \"用户名或密码错误\")\r\n\r\n"
        "  ## AuthServiceImpl.requireUser(Long id)\r\n"
        "异常: BusinessException(404, \"用户不存在\")\r\n",
        source_type="generated_java_md",
        metadata={"document_id": "auth_service", "title": "AuthServiceImpl"},
    )

    assert len(nodes) == 2
    assert nodes[0].metadata["method_name"] == "login"
    assert nodes[1].metadata["method_name"] == "requireUser"


def test_data_analysis_schema_markdown_keeps_whole_file_as_one_chunk() -> None:
    service = ChunkService(chunk_size=64, chunk_overlap=0)

    nodes = service.split_nodes(
        """
# sale_order

业务含义：订单表。

## 字段
- id: 订单ID
- order_no: 订单编号

## 索引
- PRIMARY(id)
- uk_order_no(order_no)

## 关联
- sale_payment_order.order_no -> sale_order.order_no
        """,
        source_type="data_analysis_schema_md",
        metadata={"document_id": "sale_order", "title": "sale_order"},
    )

    assert len(nodes) == 1
    assert nodes[0].metadata["chunk_type"] == "data_analysis_schema"
    assert nodes[0].metadata["table_name"] == "sale_order"
    assert "## 字段" in nodes[0].get_content()
    assert "## 索引" in nodes[0].get_content()
    assert "## 关联" in nodes[0].get_content()
