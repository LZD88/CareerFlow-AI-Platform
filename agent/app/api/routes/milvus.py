from fastapi import APIRouter

from app.services.knowledge_base.milvus_service import MilvusService

router = APIRouter()
service = MilvusService()


@router.get("/milvus/health")
def milvus_health() -> dict:
    """
    检查 Python 服务到 Milvus 的连接是否可用。

    这个接口适合部署或改配置后先调用一次：
    - 能返回 ok=true，说明 19530 端口和 Milvus 服务可访问；
    - 报连接失败，优先检查云服务器安全组、防火墙和 docker-compose ps。
    """
    return service.health()


@router.post("/milvus/collections/init")
def init_milvus_collection() -> dict:
    """
    初始化学生知识库 collection。

    只负责建表，不写测试数据。后续文档入库和 RAG 检索都会使用这个 collection。
    """
    return service.ensure_collection()


@router.post("/milvus/init-test")
def milvus_init_test() -> dict:
    """
    一键验证 Milvus 可用性。

    会创建 collection、写入一条测试向量，再立刻查询回来。
    开发阶段推荐先跑这个接口，确认 Milvus 链路没有问题。
    """
    return service.init_test()
