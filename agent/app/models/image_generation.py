from pydantic import BaseModel, Field


class ImageGenerateRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=4000)
    size: str = "1024x1024"
    quality: str = "auto"
    output_format: str = "png"


class ImageGenerateResponse(BaseModel):
    model: str
    file_path: str
    size: str
    quality: str
    output_format: str
