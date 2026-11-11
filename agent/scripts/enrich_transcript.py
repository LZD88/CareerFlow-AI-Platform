"""Enrich a transcript JSON file with deterministic text/timing features.

Example:
    uv run python scripts/enrich_transcript.py \
        /Users/huangjin/Downloads/sg1_spk.json \
        --output /Users/huangjin/Downloads/sg1_spk_enriched.json \
        --speaker-label 0=嘉宾 \
        --speaker-label 1=参哥
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any


CHINESE_CHAR_PATTERN = re.compile(r"[\u4e00-\u9fff]")
QUESTION_MARKS = ("?", "？")
EXCLAMATION_MARKS = ("!", "！")
COMMAND_HINTS = ("你要", "你得", "你必须", "你先", "一定要", "记住", "不要", "别")
CONTRAST_HINTS = ("但是", "可是", "问题是", "核心是", "不是", "而是", "其实")
MONEY_HINTS = ("钱", "万", "亿", "成本", "收入", "利润", "亏", "赚", "成交", "投")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Add deterministic features to transcript JSON segments.")
    parser.add_argument("input", help="Path to transcript JSON, such as /Users/huangjin/Downloads/sg1_spk.json")
    parser.add_argument(
        "-o",
        "--output",
        help="Path to write enriched JSON. Defaults to <input>_enriched.json",
    )
    parser.add_argument(
        "--speaker-label",
        action="append",
        default=[],
        metavar="ID=NAME",
        help="Map speaker ids to names, for example --speaker-label 0=嘉宾 --speaker-label 1=参哥.",
    )
    return parser.parse_args()


def parse_speaker_labels(raw_labels: list[str]) -> dict[int, str]:
    labels: dict[int, str] = {}
    for raw_label in raw_labels:
        speaker_id, separator, name = raw_label.partition("=")
        if not separator:
            raise ValueError(f"Invalid speaker label: {raw_label}. Expected format: ID=NAME")
        labels[int(speaker_id)] = name
    return labels


def count_chinese_chars(text: str) -> int:
    return len(CHINESE_CHAR_PATTERN.findall(text))


def classify_sentence_type(text: str) -> str:
    stripped = text.strip()
    if stripped.endswith(QUESTION_MARKS):
        return "question"
    if stripped.endswith(EXCLAMATION_MARKS):
        return "exclamation"
    if any(hint in stripped for hint in COMMAND_HINTS):
        return "instruction"
    if any(hint in stripped for hint in CONTRAST_HINTS):
        return "contrast"
    return "statement"


def collect_rule_tags(text: str) -> list[str]:
    tags: list[str] = []
    if text.endswith(QUESTION_MARKS):
        tags.append("提问")
    if any(hint in text for hint in COMMAND_HINTS):
        tags.append("行动指令")
    if any(hint in text for hint in CONTRAST_HINTS):
        tags.append("转折判断")
    if any(hint in text for hint in MONEY_HINTS):
        tags.append("商业/金钱")
    if len(text) <= 12:
        tags.append("短句")
    return tags


def enrich_segments(segments: list[dict[str, Any]], speaker_labels: dict[int, str]) -> list[dict[str, Any]]:
    enriched: list[dict[str, Any]] = []
    previous_end_by_speaker: dict[int, float] = {}
    previous_end: float | None = None

    for index, segment in enumerate(segments):
        text = str(segment.get("text", "")).strip()
        start = float(segment["start"])
        end = float(segment["end"])
        speaker = segment.get("speaker")
        speaker_id = int(speaker) if speaker is not None else None
        duration = max(round(end - start, 3), 0.001)
        chars_count = count_chinese_chars(text)

        pause_before = None if previous_end is None else round(start - previous_end, 3)
        speaker_pause_before = (
            None
            if speaker_id is None or speaker_id not in previous_end_by_speaker
            else round(start - previous_end_by_speaker[speaker_id], 3)
        )

        enriched_segment = {
            **segment,
            "index": index,
            "speaker_name": speaker_labels.get(speaker_id) if speaker_id is not None else None,
            "duration": duration,
            "pause_before": pause_before,
            "speaker_pause_before": speaker_pause_before,
            "chars_count": chars_count,
            "chars_per_second": round(chars_count / duration, 2),
            "sentence_type": classify_sentence_type(text),
            "rule_tags": collect_rule_tags(text),
        }
        enriched.append(enriched_segment)

        previous_end = end
        if speaker_id is not None:
            previous_end_by_speaker[speaker_id] = end

    return enriched


def main() -> None:
    args = parse_args()
    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input transcript file does not exist: {input_path}")

    output_path = (
        Path(args.output).expanduser().resolve()
        if args.output
        else input_path.with_name(f"{input_path.stem}_enriched.json")
    )
    segments = json.loads(input_path.read_text(encoding="utf-8"))
    speaker_labels = parse_speaker_labels(args.speaker_label)
    enriched = enrich_segments(segments, speaker_labels)
    output_path.write_text(json.dumps(enriched, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Enriched transcript saved to {output_path}")


if __name__ == "__main__":
    main()
