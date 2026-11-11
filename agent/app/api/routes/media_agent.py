from fastapi import APIRouter, HTTPException

from app.models.media_agent import MediaTextDraftRequest, MediaTextDraftResponse, MediaTopicRequest, MediaTopicResponse
from app.services.media_agent.text_draft_service import MediaTextDraftService
from app.services.media_agent.topic_service import MediaTopicService

router = APIRouter()
topic_service = MediaTopicService()
text_draft_service = MediaTextDraftService()


@router.post("/media-agent/topics", response_model=MediaTopicResponse)
def generate_media_topics(request: MediaTopicRequest) -> MediaTopicResponse:
    try:
        return topic_service.generate_topics(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/media-agent/drafts/generate-text", response_model=MediaTextDraftResponse)
def generate_media_text_draft(request: MediaTextDraftRequest) -> MediaTextDraftResponse:
    try:
        return text_draft_service.generate_text_draft(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
