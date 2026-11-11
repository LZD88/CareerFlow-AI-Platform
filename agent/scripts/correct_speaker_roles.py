"""Correct host/guest roles in a transcript JSON/TXT transcript with an LLM.

The ASR speaker id is kept as audio_speaker. The LLM adds semantic role fields:
speaker_role, speaker_name, role_confidence, and role_reason.

Example:
    uv run python scripts/correct_speaker_roles.py \
        /Users/huangjin/Downloads/sg1_spk.json \
        --output /Users/huangjin/Downloads/sg1_spk_role_corrected.json
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings


SYSTEM_PROMPT = """你是直播连麦逐字稿的说话角色校正助手。

任务：根据上下文判断每一段话的真实对话角色。

角色定义：
- host: 主播/导师/参哥。通常负责提问、追问、点评、下判断、给建议、控场、引导方案。
- guest: 连麦嘉宾/学员。通常回答问题、讲自己的投入、亏损、经历、业务现状、感谢、解释背景。
- mixed: 同一段里明显混入了 host 和 guest 两个人的话，无法不拆分就归到单一角色。
- unknown: 上下文不足，无法判断。

重要规则：
- audio_speaker 是声纹模型结果，可能不准。你可以参考它，但不要盲信。
- 必须优先根据上下文和语义判断角色。
- 必须为输入中的每个 index 返回一条结果，不能遗漏，不能增加不存在的 index。
- role_confidence 必须是 0 到 1 的数字；如果无法判断，填 0.5。
- role_reason 必须简短，最多 30 个中文字，不要展开推理过程。
- 输出必须是严格 JSON 对象，不要 Markdown，不要解释文字。

输出格式：
{
  "items": [
    {
      "index": 0,
      "speaker_role": "host",
      "speaker_name": "参哥",
      "role_confidence": 0.92,
      "role_reason": "短问句追问投入金额。"
    }
  ]
}
"""

TXT_LINE_PATTERN = re.compile(
    r"^\[(?P<start>\d{2}:\d{2}:\d{2}\.\d{3})\s+-->\s+(?P<end>\d{2}:\d{2}:\d{2}\.\d{3})\]\s+"
    r"(?:(?P<label>[^:：]+)[:：]\s*)?(?P<text>.*)$"
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Correct transcript speaker roles with an LLM.")
    parser.add_argument("input", help="Path to transcript JSON/TXT, such as /Users/huangjin/Downloads/sg1_spk.txt")
    parser.add_argument(
        "-o",
        "--output",
        help="Path to write role-corrected JSON. Defaults to <input>_role_corrected.json",
    )
    parser.add_argument(
        "--model",
        help="Override model name. Defaults to qwen_router_model for qwen provider, otherwise ARK_MODEL.",
    )
    parser.add_argument(
        "--provider",
        choices=["qwen", "ark"],
        default="qwen",
        help="LLM provider. qwen uses QWEN_API_KEY and disables thinking by default.",
    )
    parser.add_argument(
        "--max-segments",
        type=int,
        default=0,
        help="Only analyze the first N segments for testing. Default 0 means all segments.",
    )
    return parser.parse_args()


def parse_time_to_seconds(value: str) -> float:
    hours, minutes, rest = value.split(":")
    seconds, milliseconds = rest.split(".")
    return int(hours) * 3600 + int(minutes) * 60 + int(seconds) + int(milliseconds) / 1000


def load_segments(input_path: Path) -> list[dict[str, Any]]:
    if input_path.suffix.lower() == ".json":
        return json.loads(input_path.read_text(encoding="utf-8"))

    segments: list[dict[str, Any]] = []
    for line in input_path.read_text(encoding="utf-8").splitlines():
        match = TXT_LINE_PATTERN.match(line.strip())
        if not match:
            continue
        label = match.group("label")
        text = match.group("text").strip()
        segments.append(
            {
                "start": round(parse_time_to_seconds(match.group("start")), 3),
                "end": round(parse_time_to_seconds(match.group("end")), 3),
                "speaker_label": label,
                "text": text,
            }
        )
    return segments


def normalize_segments(segments: list[dict[str, Any]], max_segments: int) -> list[dict[str, Any]]:
    selected = segments[:max_segments] if max_segments > 0 else segments
    normalized = []
    for index, segment in enumerate(selected):
        normalized.append(
            {
                "index": index,
                "start": segment.get("start"),
                "end": segment.get("end"),
                "audio_speaker": segment.get("speaker"),
                "input_label": segment.get("speaker_label") or segment.get("speaker_name"),
                "text": segment.get("text", ""),
            }
        )
    return normalized


def extract_json_object(content: str) -> dict[str, Any]:
    text = content.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:].strip()
    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        text = text[start : end + 1]
    return json.loads(text)


def build_llm(provider: str, model_override: str | None) -> ChatOpenAI:
    settings = get_settings()
    if provider == "qwen":
        if not settings.qwen_api_key:
            raise ValueError("QWEN_API_KEY 未配置，无法调用千问模型")
        return ChatOpenAI(
            api_key=settings.qwen_api_key,
            base_url=settings.qwen_base_url,
            model=model_override or settings.qwen_router_model or settings.qwen_model,
            temperature=0,
            extra_body={"enable_thinking": False},
        )

    if not settings.ark_api_key:
        raise ValueError("ARK_API_KEY 未配置，无法调用火山方舟模型")
    return ChatOpenAI(
        api_key=settings.ark_api_key,
        base_url=settings.ark_base_url,
        model=model_override or settings.ark_model,
        temperature=0,
    )


def call_llm(items: list[dict[str, Any]], provider: str, model_override: str | None) -> list[dict[str, Any]]:
    llm = build_llm(provider, model_override)
    user_prompt = "请校正以下直播连麦逐字稿的 speaker_role：\n" + json.dumps(
        {"items": items},
        ensure_ascii=False,
        indent=2,
    )
    response = llm.invoke(
        [
            ("system", SYSTEM_PROMPT),
            ("user", user_prompt),
        ]
    )
    content = response.content if isinstance(response.content, str) else json.dumps(response.content, ensure_ascii=False)
    data = extract_json_object(content)
    return data.get("items", [])


def merge_roles(segments: list[dict[str, Any]], role_items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    role_by_index = {int(item["index"]): item for item in role_items if "index" in item}
    corrected = []
    for index, segment in enumerate(segments):
        role_item = role_by_index.get(index)
        if not role_item:
            corrected.append(
                {
                    **segment,
                    "audio_speaker": segment.get("speaker"),
                    "input_speaker_label": segment.get("speaker_label") or segment.get("speaker_name"),
                    "speaker_role": "unknown",
                    "speaker_name": "未知",
                    "role_confidence": 0.0,
                    "role_reason": "LLM 未返回该段结果。",
                }
            )
            continue

        corrected.append(
            {
                **segment,
                "audio_speaker": segment.get("speaker"),
                "input_speaker_label": segment.get("speaker_label") or segment.get("speaker_name"),
                "speaker_role": role_item.get("speaker_role", "unknown"),
                "speaker_name": role_item.get("speaker_name", "未知"),
                "role_confidence": role_item.get("role_confidence"),
                "role_reason": role_item.get("role_reason", ""),
            }
        )
    return corrected


def main() -> None:
    args = parse_args()
    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input transcript file does not exist: {input_path}")

    output_path = (
        Path(args.output).expanduser().resolve()
        if args.output
        else input_path.with_name(f"{input_path.stem}_role_corrected.json")
    )
    segments = load_segments(input_path)
    items = normalize_segments(segments, args.max_segments)
    role_items = call_llm(items, args.provider, args.model)
    corrected = merge_roles(segments[: len(items)], role_items)
    output_path.write_text(json.dumps(corrected, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Role-corrected transcript saved to {output_path}")


if __name__ == "__main__":
    main()
