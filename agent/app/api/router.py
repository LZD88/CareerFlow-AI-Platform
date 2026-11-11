from fastapi import APIRouter

from app.api.routes.diagnose import router as diagnose_router
from app.api.routes.data_analysis import router as data_analysis_router
from app.api.routes.embeddings import router as embeddings_router
from app.api.routes.images import router as images_router
from app.api.routes.interview_review import router as interview_review_router
from app.api.routes.knowledge_base import router as knowledge_base_router
from app.api.routes.media_agent import router as media_agent_router
from app.api.routes.milvus import router as milvus_router
from app.api.routes.mock_interview import router as mock_interview_router
from app.api.routes.student_chat import router as student_chat_router

api_router = APIRouter()
api_router.include_router(data_analysis_router, prefix="/api", tags=["data-analysis"])
api_router.include_router(diagnose_router, prefix="/api", tags=["diagnose"])
api_router.include_router(embeddings_router, prefix="/api", tags=["embeddings"])
api_router.include_router(images_router, prefix="/api", tags=["images"])
api_router.include_router(interview_review_router, prefix="/api", tags=["interview-review"])
api_router.include_router(knowledge_base_router, prefix="/api", tags=["knowledge-base"])
api_router.include_router(media_agent_router, prefix="/api", tags=["media-agent"])
api_router.include_router(milvus_router, prefix="/api", tags=["milvus"])
api_router.include_router(mock_interview_router, prefix="/api", tags=["mock-interview"])
api_router.include_router(student_chat_router, prefix="/api", tags=["student-chat"])
