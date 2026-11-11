import gzip
import json
import struct
from dataclasses import dataclass
from typing import Any


EVENT_START_CONNECTION = 1
EVENT_FINISH_CONNECTION = 2
EVENT_START_SESSION = 100
EVENT_FINISH_SESSION = 102
EVENT_TASK_REQUEST = 200
EVENT_SAY_HELLO = 300
EVENT_END_ASR = 400
EVENT_CHAT_TEXT_QUERY = 501

EVENT_TTS_RESPONSE = 352

MESSAGE_TYPE_FULL_CLIENT_REQUEST = 0b0001
MESSAGE_TYPE_AUDIO_ONLY_REQUEST = 0b0010
MESSAGE_TYPE_FULL_SERVER_RESPONSE = 0b1001
MESSAGE_TYPE_AUDIO_ONLY_RESPONSE = 0b1011
MESSAGE_TYPE_ERROR = 0b1111

FLAGS_EVENT = 0b0100

SERIALIZATION_RAW = 0b0000
SERIALIZATION_JSON = 0b0001

COMPRESSION_NONE = 0b0000
COMPRESSION_GZIP = 0b0001


@dataclass
class VolcRealtimeFrame:
    message_type: int
    event: int | None
    session_id: str | None
    payload: bytes
    payload_json: dict[str, Any] | None
    error_code: int | None = None


def build_json_event(event: int, payload: dict[str, Any], session_id: str | None = None) -> bytes:
    body = json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return _build_frame(MESSAGE_TYPE_FULL_CLIENT_REQUEST, SERIALIZATION_JSON, event, body, session_id)


def build_audio_event(event: int, payload: bytes, session_id: str) -> bytes:
    return _build_frame(MESSAGE_TYPE_AUDIO_ONLY_REQUEST, SERIALIZATION_RAW, event, payload, session_id)


def _build_frame(message_type: int, serialization: int, event: int, payload: bytes, session_id: str | None) -> bytes:
    header = bytes(
        [
            0x11,
            (message_type << 4) | FLAGS_EVENT,
            (serialization << 4) | COMPRESSION_NONE,
            0x00,
        ]
    )
    optional = struct.pack(">i", event)
    if session_id:
        session_bytes = session_id.encode("utf-8")
        optional += struct.pack(">i", len(session_bytes)) + session_bytes
    return header + optional + struct.pack(">i", len(payload)) + payload


def parse_frame(data: bytes) -> VolcRealtimeFrame:
    if len(data) < 8:
        raise ValueError("火山 Realtime 数据帧长度不足")

    message_type = data[1] >> 4
    flags = data[1] & 0x0F
    serialization = data[2] >> 4
    compression = data[2] & 0x0F
    offset = 4
    event = None
    error_code = None
    session_id = None

    if message_type == MESSAGE_TYPE_ERROR:
        error_code = struct.unpack(">i", data[offset : offset + 4])[0]
        offset += 4

    if flags & FLAGS_EVENT:
        event = struct.unpack(">i", data[offset : offset + 4])[0]
        offset += 4

    if offset + 4 > len(data):
        return VolcRealtimeFrame(message_type, event, session_id, b"", None, error_code)

    possible_session_size = struct.unpack(">i", data[offset : offset + 4])[0]
    if 0 < possible_session_size <= 1024 and offset + 4 + possible_session_size + 4 <= len(data):
        session_start = offset + 4
        session_end = session_start + possible_session_size
        session_id = data[session_start:session_end].decode("utf-8", errors="ignore")
        offset = session_end

    payload_size = struct.unpack(">i", data[offset : offset + 4])[0]
    offset += 4
    payload = data[offset : offset + payload_size]
    if compression == COMPRESSION_GZIP and payload:
        payload = gzip.decompress(payload)

    payload_json = None
    if serialization == SERIALIZATION_JSON and payload:
        payload_json = json.loads(payload.decode("utf-8"))

    return VolcRealtimeFrame(message_type, event, session_id, payload, payload_json, error_code)
