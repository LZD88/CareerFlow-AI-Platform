from fastapi import APIRouter, HTTPException

from app.models.knowledge_base import (
    KnowledgeDeleteDocumentChunksRequest,
    KnowledgeDeleteDocumentChunksResponse,
    KnowledgeDocumentChunksRequest,
    KnowledgeDocumentChunksResponse,
    KnowledgeIngestTextRequest,
    KnowledgeIngestTextResponse,
    KnowledgeSearchRequest,
    KnowledgeSearchResponse,
)
from app.services.knowledge_base.document_chunk_service import DocumentChunkService
from app.services.knowledge_base.knowledge_ingest_service import KnowledgeIngestService
from app.services.knowledge_base.knowledge_search_service import KnowledgeSearchService

router = APIRouter()
ingest_service = KnowledgeIngestService()
search_service = KnowledgeSearchService()
document_chunk_service = DocumentChunkService()


@router.post("/knowledge-base/ingest-text", response_model=KnowledgeIngestTextResponse)
def ingest_text(request: KnowledgeIngestTextRequest) -> dict:
    """
    文本知识入库接口。

    第一版用于验证 RAG 入库闭环：文本 -> chunk -> embedding -> Milvus。
    后续 Java 管理页面可以先调用这个接口，再逐步扩展成文件入库。
    """
    try:
        return ingest_service.ingest_text(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/knowledge-base/search", response_model=KnowledgeSearchResponse)
def search_knowledge(request: KnowledgeSearchRequest) -> dict:
    """
    知识库检索接口。

    用于验证 RAG 检索闭环：query -> embedding -> Milvus search -> topK chunks。
    """
    try:
        return search_service.search(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/knowledge-base/document-chunks", response_model=KnowledgeDocumentChunksResponse)
def list_document_chunks(request: KnowledgeDocumentChunksRequest) -> dict:
    """
    查询某份文档在 Milvus 中的 chunk 明细。
    """
    try:
        return document_chunk_service.list_document_chunks(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/knowledge-base/delete-document-chunks", response_model=KnowledgeDeleteDocumentChunksResponse)
def delete_document_chunks(request: KnowledgeDeleteDocumentChunksRequest) -> dict:
    """
    删除某份文档在 Milvus 中的 chunk。
    """
    try:
        return document_chunk_service.delete_document_chunks(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
