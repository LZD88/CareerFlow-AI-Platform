from fastapi import APIRouter, HTTPException

from app.models.image_generation import ImageGenerateRequest, ImageGenerateResponse
from app.services.image_generation.openai_image_service import OpenAIImageService

router = APIRouter()
service = OpenAIImageService()


@router.post("/images/generate", response_model=ImageGenerateResponse)
def generate_image(request: ImageGenerateRequest) -> ImageGenerateResponse:
    try:
        return service.generate(request)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
