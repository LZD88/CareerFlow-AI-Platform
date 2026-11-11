import base64
import re
from pathlib import Path
from uuid import uuid4

from openai import OpenAI

from app.config.settings import get_settings
from app.models.image_generation import ImageGenerateRequest, ImageGenerateResponse


class OpenAIImageService:
    _SUPPORTED_FORMATS = {"png", "jpeg", "webp"}

    def __init__(self) -> None:
        self.settings = get_settings()
        self.output_dir = Path("generated-images")

    def generate(self, request: ImageGenerateRequest) -> ImageGenerateResponse:
        if not self.settings.openai_api_key:
            raise ValueError("OPENAI_API_KEY 未配置，无法调用 OpenAI 图片生成接口")

        output_format = request.output_format.lower()
        if output_format not in self._SUPPORTED_FORMATS:
            raise ValueError("output_format 只支持 png、jpeg、webp")

        client = OpenAI(api_key=self.settings.openai_api_key)
        result = client.images.generate(
            model=self.settings.openai_image_model,
            prompt=request.prompt,
            size=request.size,
            quality=request.quality,
            output_format=output_format,
        )

        if not result.data or not result.data[0].b64_json:
            raise ValueError("OpenAI 未返回图片数据")

        image_bytes = base64.b64decode(result.data[0].b64_json)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        filename = f"{self._slugify(request.prompt)}-{uuid4().hex[:8]}.{output_format}"
        file_path = self.output_dir / filename
        file_path.write_bytes(image_bytes)

        return ImageGenerateResponse(
            model=self.settings.openai_image_model,
            file_path=str(file_path.resolve()),
            size=request.size,
            quality=request.quality,
            output_format=output_format,
        )

    def _slugify(self, prompt: str) -> str:
        slug = re.sub(r"[^a-zA-Z0-9]+", "-", prompt.strip().lower()).strip("-")
        return slug[:48] or "openai-image"
