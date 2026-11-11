import logging
import tempfile
import time
import uuid
from pathlib import Path
from threading import Lock

import httpx

from app.config.settings import get_settings
from app.models.interview_review import InterviewTranscriptSegment, InterviewTranscribeRequest, InterviewTranscribeResponse

BREAK_CHARS = set("。！？!?；;")
SOFT_BREAK_CHARS = set("，,、")
PUNCTUATION_CHARS = BREAK_CHARS | SOFT_BREAK_CHARS | set("：:“”\"'（）()《》<>【】[]")
VOLC_SUCCESS = "20000000"
VOLC_PROCESSING = {"20000001", "20000002"}
SUPPORTED_VOLC_FORMATS = {"mp3", "wav", "ogg"}
logger = logging.getLogger(__name__)


def rich_transcription_postprocess(text: str) -> str:
    from funasr.utils.postprocess_utils import rich_transcription_postprocess as postprocess
    return postprocess(text)


class InterviewTranscribeService:
    def __init__(self) -> None:
        self._fast_model = None
        self._speaker_model = None
        self._model_lock = Lock()

    def transcribe(self, request: InterviewTranscribeRequest) -> InterviewTranscribeResponse:
        settings = get_settings()
        use_volcengine = self._use_volcengine()
        logger.info(
            "面试转写 provider=%s use_volcengine=%s task_id=%s speaker_diarization=%s",
            settings.interview_asr_provider,
            use_volcengine,
            request.taskId,
            request.speakerDiarization,
        )
        if use_volcengine:
            return self._transcribe_by_volcengine(request)
        return self._transcribe_by_funasr(request)

    def _use_volcengine(self) -> bool:
        return get_settings().interview_asr_provider.lower() in {"volcengine", "volc", "doubao"}

    def _transcribe_by_volcengine(self, request: InterviewTranscribeRequest) -> InterviewTranscribeResponse:
        settings = get_settings()
        logger.info(
            "火山语音配置检查 task_id=%s has_api_key=%s has_app_key=%s has_access_key=%s resource_id=%s",
            request.taskId,
            bool(settings.volc_speech_api_key),
            bool(settings.volc_speech_app_key),
            bool(settings.volc_speech_access_key),
            settings.volc_speech_resource_id,
        )
        if not settings.volc_speech_api_key and not (settings.volc_speech_app_key and settings.volc_speech_access_key):
            raise ValueError("未配置火山语音识别 Key，请设置 VOLC_SPEECH_API_KEY 或 VOLC_SPEECH_APP_KEY/VOLC_SPEECH_ACCESS_KEY")

        task_id = str(uuid.uuid4())
        logger.info("提交火山语音转写 task_id=%s volc_task_id=%s", request.taskId, task_id)
        submit_log_id = self._submit_volcengine_task(request, task_id)
        logger.info("火山语音提交成功 task_id=%s volc_task_id=%s log_id=%s", request.taskId, task_id, submit_log_id)
        result = self._wait_volcengine_result(task_id, submit_log_id)
        segments = self._extract_volcengine_segments(result) if request.speakerDiarization else []
        if segments:
            segments = self._merge_same_speaker_segments(segments)
            text = self._format_segments_text(segments)
        else:
            text = (result.get("result", {}).get("text") or "").strip()
        return InterviewTranscribeResponse(
            taskId=request.taskId,
            audioObjectKey=request.audioObjectKey,
            text=text,
            segments=segments,
        )

    def _submit_volcengine_task(self, request: InterviewTranscribeRequest, task_id: str) -> str:
        settings = get_settings()
        response = self._post_volcengine(
            settings.volc_speech_submit_url,
            headers=self._build_volcengine_headers(task_id),
            json=self._build_volcengine_payload(request),
            log_name="submit",
            task_id=task_id,
        )
        status_code = response.headers.get("X-Api-Status-Code", "")
        logger.info(
            "火山语音提交返回 task_id=%s volc_task_id=%s http_status=%s api_status=%s message=%s",
            request.taskId,
            task_id,
            response.status_code,
            status_code,
            response.headers.get("X-Api-Message", ""),
        )
        if status_code != VOLC_SUCCESS:
            message = response.headers.get("X-Api-Message", response.text)
            raise ValueError(f"火山语音提交失败: code={status_code}, message={message}")
        return response.headers.get("X-Tt-Logid", "")

    def _wait_volcengine_result(self, task_id: str, submit_log_id: str) -> dict:
        settings = get_settings()
        deadline = time.monotonic() + settings.volc_speech_max_wait_s
        last_message = ""
        query_count = 0
        while time.monotonic() < deadline:
            query_count += 1
            headers = self._build_volcengine_headers(task_id)
            if submit_log_id:
                headers["X-Tt-Logid"] = submit_log_id
            response = self._post_volcengine(
                settings.volc_speech_query_url,
                headers=headers,
                json={},
                log_name="query",
                task_id=task_id,
            )
            status_code = response.headers.get("X-Api-Status-Code", "")
            last_message = response.headers.get("X-Api-Message", "")
            logger.info(
                "火山语音查询返回 volc_task_id=%s query_count=%s http_status=%s api_status=%s message=%s",
                task_id,
                query_count,
                response.status_code,
                status_code,
                last_message,
            )
            if status_code == VOLC_SUCCESS:
                return response.json()
            if status_code not in VOLC_PROCESSING:
                raise ValueError(f"火山语音查询失败: code={status_code}, message={last_message or response.text}")
            time.sleep(settings.volc_speech_poll_interval_s)
        raise ValueError(f"火山语音转写超时: task_id={task_id}, message={last_message}")

    def _post_volcengine(self, url: str, headers: dict[str, str], json: dict, log_name: str, task_id: str) -> httpx.Response:
        settings = get_settings()
        last_error: httpx.RequestError | None = None
        for attempt in range(1, settings.volc_speech_max_retries + 1):
            try:
                with httpx.Client(
                    timeout=settings.volc_speech_request_timeout_s,
                    trust_env=settings.volc_speech_trust_env_proxy,
                ) as client:
                    response = client.post(url, headers=headers, json=json)
                    response.raise_for_status()
                    return response
            except httpx.RequestError as ex:
                last_error = ex
                logger.warning(
                    "火山语音网络异常，准备重试 name=%s volc_task_id=%s attempt=%s/%s error=%s",
                    log_name,
                    task_id,
                    attempt,
                    settings.volc_speech_max_retries,
                    ex,
                )
                if attempt < settings.volc_speech_max_retries:
                    time.sleep(min(attempt, 5))
        if last_error is not None:
            raise last_error
        raise RuntimeError("火山语音请求失败")

    def _build_volcengine_headers(self, task_id: str) -> dict[str, str]:
        settings = get_settings()
        headers = {
            "Content-Type": "application/json",
            "X-Api-Resource-Id": settings.volc_speech_resource_id,
            "X-Api-Request-Id": task_id,
            "X-Api-Sequence": "-1",
        }
        if settings.volc_speech_api_key:
            headers["X-Api-Key"] = settings.volc_speech_api_key
        else:
            headers["X-Api-App-Key"] = settings.volc_speech_app_key
            headers["X-Api-Access-Key"] = settings.volc_speech_access_key
        return headers

    def _build_volcengine_payload(self, request: InterviewTranscribeRequest) -> dict:
        settings = get_settings()
        audio: dict[str, object] = {"url": request.audioUrl}
        audio_format = self._detect_volcengine_audio_format(request.audioObjectKey, request.audioUrl)
        if audio_format:
            audio["format"] = audio_format
        volc_request: dict[str, object] = {
            "model_name": settings.volc_speech_model_name,
            "enable_itn": True,
            "enable_punc": True,
            "enable_ddc": True,
            "show_utterances": True,
            "enable_speaker_info": request.speakerDiarization,
        }
        if settings.volc_speech_model_version:
            volc_request["model_version"] = settings.volc_speech_model_version
        if request.speakerDiarization and settings.volc_speech_ssd_version:
            volc_request["ssd_version"] = settings.volc_speech_ssd_version
        if request.language and request.language != "auto":
            audio["language"] = request.language
        return {
            "user": {"uid": settings.volc_speech_api_key or settings.volc_speech_app_key or "yq-ai"},
            "audio": audio,
            "request": volc_request,
        }

    def _detect_volcengine_audio_format(self, audio_object_key: str, audio_url: str) -> str | None:
        suffix = Path(audio_object_key).suffix.lower().lstrip(".")
        if not suffix:
            suffix = Path(audio_url.split("?", 1)[0]).suffix.lower().lstrip(".")
        if suffix in SUPPORTED_VOLC_FORMATS:
            return suffix
        return None

    def _extract_volcengine_segments(self, result: dict) -> list[InterviewTranscriptSegment]:
        segments: list[InterviewTranscriptSegment] = []
        utterances = result.get("result", {}).get("utterances") or []
        for utterance in utterances:
            if not isinstance(utterance, dict):
                continue
            text = (utterance.get("text") or "").strip()
            if not text:
                continue
            additions = utterance.get("additions") or {}
            speaker = additions.get("speaker") or utterance.get("speaker") or utterance.get("speaker_id")
            segments.append(
                InterviewTranscriptSegment(
                    start=round((utterance.get("start_time") or 0) / 1000, 3),
                    end=round((utterance.get("end_time") or 0) / 1000, 3),
                    speaker=speaker,
                    speakerName=self._speaker_name(speaker),
                    text=text,
                )
            )
        return segments

    def _transcribe_by_funasr(self, request: InterviewTranscribeRequest) -> InterviewTranscribeResponse:
        # 云端 ASR 和文本服务不依赖本地 Torch；只在选择 FunASR 时加载。
        try:
            import funasr  # noqa: F401
        except ImportError as exc:
            raise ValueError("本地语音依赖未安装，请运行 uv sync --extra local-asr") from exc
        logger.info("使用 FunASR 本地转写 task_id=%s speaker_diarization=%s", request.taskId, request.speakerDiarization)
        with tempfile.TemporaryDirectory(prefix="interview-audio-") as tmp_dir:
            audio_path = self._download_audio(request.audioUrl, request.audioObjectKey, Path(tmp_dir))
            model = self._get_model(request.speakerDiarization)
            result = model.generate(
                input=str(audio_path),
                language=request.language or "auto",
                use_itn=True,
                batch_size_s=get_settings().interview_asr_batch_size_s,
            )
            segments = self._extract_segments(result) if request.speakerDiarization else []
            if segments:
                segments = self._merge_same_speaker_segments(segments)
                text = self._format_segments_text(segments)
            else:
                text = self._extract_text(result)
            return InterviewTranscribeResponse(
                taskId=request.taskId,
                audioObjectKey=request.audioObjectKey,
                text=text,
                segments=segments,
            )

    def _get_model(self, speaker_diarization: bool):
        from funasr import AutoModel
        if speaker_diarization and self._speaker_model is not None:
            return self._speaker_model
        if not speaker_diarization and self._fast_model is not None:
            return self._fast_model
        with self._model_lock:
            if speaker_diarization and self._speaker_model is not None:
                return self._speaker_model
            if not speaker_diarization and self._fast_model is not None:
                return self._fast_model
            settings = get_settings()
            model_kwargs = {
                "model": settings.interview_asr_speaker_model if speaker_diarization else settings.interview_asr_model,
                "vad_model": settings.interview_asr_vad_model,
                "vad_kwargs": {"max_single_segment_time": settings.interview_asr_max_single_segment_time_ms},
                "device": settings.interview_asr_device,
                "disable_update": True,
            }
            if speaker_diarization:
                model_kwargs["punc_model"] = settings.interview_asr_punc_model
                model_kwargs["spk_model"] = settings.interview_asr_spk_model
                self._speaker_model = AutoModel(**model_kwargs)
                return self._speaker_model
            self._fast_model = AutoModel(**model_kwargs)
            return self._fast_model

    def _download_audio(self, audio_url: str, audio_object_key: str, tmp_dir: Path) -> Path:
        suffix = Path(audio_object_key).suffix or ".audio"
        audio_path = tmp_dir / f"interview{suffix}"
        logger.info("下载面试音频 object_key=%s tmp_path=%s", audio_object_key, audio_path)
        with httpx.stream("GET", audio_url, timeout=get_settings().interview_audio_download_timeout_s) as response:
            response.raise_for_status()
            with audio_path.open("wb") as file:
                for chunk in response.iter_bytes():
                    if chunk:
                        file.write(chunk)
        return audio_path

    def _extract_text(self, result) -> str:
        if not result:
            return ""
        first = result[0] if isinstance(result, list) else result
        raw_text = first.get("text", "") if isinstance(first, dict) else str(first)
        return rich_transcription_postprocess(raw_text).strip()

    def _extract_segments(self, result) -> list[InterviewTranscriptSegment]:
        sentence_segments = self._build_segments_from_sentence_info(result)
        if sentence_segments:
            return sentence_segments
        return self._build_segments_from_timestamps(result)

    def _build_segments_from_sentence_info(self, result) -> list[InterviewTranscriptSegment]:
        segments: list[InterviewTranscriptSegment] = []
        for item in result if isinstance(result, list) else [result]:
            if not isinstance(item, dict):
                continue
            for sentence in item.get("sentence_info") or []:
                text = (sentence.get("text") or "").strip()
                if not text:
                    continue
                speaker = sentence.get("spk")
                segments.append(
                    InterviewTranscriptSegment(
                        start=round(sentence.get("start", 0) / 1000, 3),
                        end=round(sentence.get("end", 0) / 1000, 3),
                        speaker=speaker,
                        speakerName=self._speaker_name(speaker),
                        text=rich_transcription_postprocess(text),
                    )
                )
        return segments

    def _build_segments_from_timestamps(self, result) -> list[InterviewTranscriptSegment]:
        segments: list[InterviewTranscriptSegment] = []
        max_segment_chars = get_settings().interview_asr_max_segment_chars

        for item in result if isinstance(result, list) else [result]:
            if not isinstance(item, dict):
                continue
            text = item.get("text", "")
            timestamps = item.get("timestamp") or []
            timestamp_index = 0
            current_chars: list[str] = []
            current_start_ms: int | None = None
            current_end_ms: int | None = None

            def flush() -> None:
                nonlocal current_chars, current_start_ms, current_end_ms
                segment_text = rich_transcription_postprocess("".join(current_chars).strip())
                if segment_text and current_start_ms is not None and current_end_ms is not None:
                    segments.append(
                        InterviewTranscriptSegment(
                            start=round(current_start_ms / 1000, 3),
                            end=round(current_end_ms / 1000, 3),
                            speaker=None,
                            speakerName=None,
                            text=segment_text,
                        )
                    )
                current_chars = []
                current_start_ms = None
                current_end_ms = None

            for char in text:
                current_chars.append(char)
                if char.isspace() or char in PUNCTUATION_CHARS:
                    if current_end_ms is not None and self._should_split(char, "".join(current_chars), max_segment_chars):
                        flush()
                    continue

                if timestamp_index < len(timestamps):
                    start_ms, end_ms = timestamps[timestamp_index]
                    timestamp_index += 1
                    if current_start_ms is None:
                        current_start_ms = start_ms
                    current_end_ms = end_ms

                if self._should_split(char, "".join(current_chars), max_segment_chars):
                    flush()

            flush()
        return segments

    def _merge_same_speaker_segments(self, segments: list[InterviewTranscriptSegment]) -> list[InterviewTranscriptSegment]:
        if not segments:
            return []
        max_segment_chars = get_settings().interview_asr_max_segment_chars
        max_merge_pause = get_settings().interview_asr_max_merge_pause_s
        merged = [segments[0].model_copy()]
        for segment in segments[1:]:
            previous = merged[-1]
            if not self._can_merge_segments(previous, segment, max_segment_chars, max_merge_pause):
                merged.append(segment.model_copy())
                continue
            previous.end = segment.end
            previous.text += segment.text
        return merged

    def _can_merge_segments(
        self,
        previous: InterviewTranscriptSegment,
        current: InterviewTranscriptSegment,
        max_segment_chars: int,
        max_merge_pause: float,
    ) -> bool:
        if previous.speaker != current.speaker:
            return False
        if previous.end is None or current.start is None:
            return False
        pause = current.start - previous.end
        if pause < 0 or pause > max_merge_pause:
            return False
        if len(previous.text) + len(current.text) > max_segment_chars:
            return False
        return True

    def _format_segments_text(self, segments: list[InterviewTranscriptSegment]) -> str:
        lines: list[str] = []
        for segment in segments:
            time_range = ""
            if segment.start is not None and segment.end is not None:
                time_range = f"[{self._format_time(segment.start)} - {self._format_time(segment.end)}] "
            speaker = segment.speakerName or "未知说话人"
            lines.append(f"{time_range}{speaker}：{segment.text}")
        return "\n".join(lines)

    def _speaker_name(self, speaker) -> str | None:
        if speaker is None:
            return None
        return f"说话人{speaker}"

    def _format_time(self, seconds: float) -> str:
        milliseconds = int(round(seconds * 1000))
        hours, remainder = divmod(milliseconds, 3_600_000)
        minutes, remainder = divmod(remainder, 60_000)
        secs, milliseconds = divmod(remainder, 1000)
        return f"{hours:02d}:{minutes:02d}:{secs:02d}.{milliseconds:03d}"

    def _should_split(self, char: str, current_text: str, max_segment_chars: int) -> bool:
        if char in BREAK_CHARS:
            return True
        if char in SOFT_BREAK_CHARS and len(current_text) >= max_segment_chars:
            return True
        return len(current_text) >= max_segment_chars * 2
