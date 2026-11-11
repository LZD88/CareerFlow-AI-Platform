import base64
import hashlib
import hmac
import json
import time
from dataclasses import dataclass

from app.config.settings import get_settings


@dataclass(frozen=True)
class MockInterviewTokenClaims:
    session_id: int
    student_id: int
    student_code: str
    interview_role: str
    volc_session_id: str
    exp: int


class MockInterviewRealtimeTokenVerifier:
    def verify(self, token: str) -> MockInterviewTokenClaims:
        if not token:
            raise ValueError("实时面试 token 不能为空")

        parts = token.split(".")
        if len(parts) != 3:
            raise ValueError("实时面试 token 格式不正确")

        signing_input = f"{parts[0]}.{parts[1]}"
        expected_signature = self._sign(signing_input)
        if not hmac.compare_digest(expected_signature, parts[2]):
            raise ValueError("实时面试 token 签名无效")

        payload = json.loads(self._base64_url_decode(parts[1]).decode("utf-8"))
        exp = int(payload.get("exp") or 0)
        if exp <= int(time.time()):
            raise ValueError("实时面试 token 已过期")

        return MockInterviewTokenClaims(
            session_id=int(payload["sessionId"]),
            student_id=int(payload["studentId"]),
            student_code=str(payload["studentCode"]),
            interview_role=str(payload.get("interviewRole") or "Java后端开发工程师"),
            volc_session_id=str(payload["volcSessionId"]),
            exp=exp,
        )

    def _sign(self, signing_input: str) -> str:
        secret = get_settings().mock_interview_realtime_token_secret.encode("utf-8")
        digest = hmac.new(secret, signing_input.encode("utf-8"), hashlib.sha256).digest()
        return base64.urlsafe_b64encode(digest).rstrip(b"=").decode("ascii")

    def _base64_url_decode(self, value: str) -> bytes:
        padding = "=" * (-len(value) % 4)
        return base64.urlsafe_b64decode(value + padding)
