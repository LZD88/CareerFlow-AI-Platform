import logging

import httpx
from fastapi import APIRouter, HTTPException

from app.models.interview_review import (
    InterviewReviewGenerateRequest,
    InterviewReviewGenerateResponse,
    InterviewQuestionExtractRequest,
    InterviewQuestionExtractResponse,
    InterviewTranscribeRequest,
    InterviewTranscribeResponse,
)
from app.services.interview_review.question_service import InterviewQuestionExtractService
from app.services.interview_review.review_service import InterviewReviewGenerateService
from app.services.interview_review.transcribe_service import InterviewTranscribeService

router = APIRouter()
transcribe_service = InterviewTranscribeService()
review_generate_service = InterviewReviewGenerateService()
question_extract_service = InterviewQuestionExtractService()
logger = logging.getLogger(__name__)


@router.post("/interview-review/transcribe", response_model=InterviewTranscribeResponse)
def transcribe_interview_audio(request: InterviewTranscribeRequest) -> InterviewTranscribeResponse:
    logger.info(
        "收到面试音频转写请求 task_id=%s object_key=%s speaker_diarization=%s",
        request.taskId,
        request.audioObjectKey,
        request.speakerDiarization,
    )
    try:
        response = transcribe_service.transcribe(request)
        logger.info(
            "面试音频转写完成 task_id=%s text_len=%s segment_count=%s",
            request.taskId,
            len(response.text or ""),
            len(response.segments or []),
        )
        return response
    except httpx.HTTPStatusError as ex:
        logger.exception("面试音频转写 HTTP 调用失败 task_id=%s status=%s", request.taskId, ex.response.status_code)
        raise HTTPException(status_code=400, detail=f"面试音频转写 HTTP 调用失败: {ex.response.status_code}") from ex
    except httpx.RequestError as ex:
        logger.exception("面试音频转写网络异常 task_id=%s", request.taskId)
        raise HTTPException(status_code=502, detail=f"面试音频转写网络异常: {ex}") from ex
    except ValueError as ex:
        logger.exception("面试音频转写参数或服务异常 task_id=%s", request.taskId)
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/interview-review/generate-review", response_model=InterviewReviewGenerateResponse)
def generate_interview_review(request: InterviewReviewGenerateRequest) -> InterviewReviewGenerateResponse:
    logger.info(
        "收到面试复盘生成请求 task_id=%s role=%s transcript_len=%s",
        request.taskId,
        request.interviewRole,
        len(request.transcriptText or ""),
    )
    try:
        return review_generate_service.generate(request)
    except ValueError as ex:
        logger.exception("面试复盘生成失败 task_id=%s", request.taskId)
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/interview-review/extract-questions", response_model=InterviewQuestionExtractResponse)
def extract_interview_questions(request: InterviewQuestionExtractRequest) -> InterviewQuestionExtractResponse:
    logger.info(
        "收到面试题提取请求 task_id=%s role=%s transcript_len=%s resume_len=%s",
        request.taskId,
        request.interviewRole,
        len(request.transcriptText or ""),
        len(request.resumeText or ""),
    )
    try:
        return question_extract_service.extract(request)
    except ValueError as ex:
        logger.exception("面试题提取失败 task_id=%s", request.taskId)
        raise HTTPException(status_code=400, detail=str(ex)) from ex
