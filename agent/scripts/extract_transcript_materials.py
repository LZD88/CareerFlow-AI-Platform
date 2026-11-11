"""Extract base material libraries from a content-labeled transcript.

Example:
    uv run python scripts/extract_transcript_materials.py \
        /Users/huangjin/Downloads/sg1_spk_content_labeled_qwen.json \
        --output-prefix /Users/huangjin/Downloads/sg1
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Extract host style references, guest cases, and stats.")
    parser.add_argument(
        "input",
        help="Path to content-labeled JSON, such as /Users/huangjin/Downloads/sg1_spk_content_labeled_qwen.json",
    )
    parser.add_argument(
        "--output-prefix",
        help="Output prefix. Defaults to the input path without _spk_content_labeled_qwen suffix.",
    )
    return parser.parse_args()


def default_output_prefix(input_path: Path) -> Path:
    stem = input_path.stem
    for suffix in ("_spk_content_labeled_qwen", "_content_labeled_qwen", "_content_labeled"):
        if stem.endswith(suffix):
            stem = stem[: -len(suffix)]
            break
    return input_path.with_name(stem)


def compact_segment(segment: dict[str, Any]) -> dict[str, Any]:
    return {
        "start": segment.get("start"),
        "end": segment.get("end"),
        "speaker_role": segment.get("speaker_role"),
        "speaker_name": segment.get("speaker_name"),
        "text": segment.get("text"),
        "scene": segment.get("scene"),
        "speech_act": segment.get("speech_act"),
        "conversation_stage": segment.get("conversation_stage"),
        "business_topic": segment.get("business_topic"),
        "business_domain": segment.get("business_domain"),
        "tone": segment.get("tone"),
        "style_tags": segment.get("style_tags", []),
        "content_summary": segment.get("content_summary"),
    }


def extract_host_style_references(segments: list[dict[str, Any]]) -> list[dict[str, Any]]:
    references = []
    for index, segment in enumerate(segments):
        if segment.get("speaker_role") != "host":
            continue
        if not segment.get("is_host_style_reference"):
            continue
        references.append(
            {
                "source_index": index,
                "source_type": "host_style_reference",
                **compact_segment(segment),
            }
        )
    return references


def extract_guest_cases(segments: list[dict[str, Any]]) -> list[dict[str, Any]]:
    cases = []
    for index, segment in enumerate(segments):
        if segment.get("speaker_role") != "guest":
            continue
        cases.append(
            {
                "source_index": index,
                "source_type": "guest_case",
                **compact_segment(segment),
            }
        )
    return cases


def count_values(segments: list[dict[str, Any]], field: str) -> dict[str, int]:
    return dict(Counter(str(segment.get(field) or "未知") for segment in segments).most_common())


def count_style_tags(segments: list[dict[str, Any]]) -> dict[str, int]:
    counter: Counter[str] = Counter()
    for segment in segments:
        counter.update(str(tag) for tag in segment.get("style_tags", []))
    return dict(counter.most_common())


def build_stats(
    segments: list[dict[str, Any]],
    host_style_references: list[dict[str, Any]],
    guest_cases: list[dict[str, Any]],
) -> dict[str, Any]:
    host_segments = [segment for segment in segments if segment.get("speaker_role") == "host"]
    guest_segments = [segment for segment in segments if segment.get("speaker_role") == "guest"]
    return {
        "total_segments": len(segments),
        "host_segments": len(host_segments),
        "guest_segments": len(guest_segments),
        "mixed_segments": sum(1 for segment in segments if segment.get("speaker_role") == "mixed"),
        "host_style_reference_count": len(host_style_references),
        "guest_case_count": len(guest_cases),
        "speaker_role_counts": count_values(segments, "speaker_role"),
        "speech_act_counts": count_values(segments, "speech_act"),
        "conversation_stage_counts": count_values(segments, "conversation_stage"),
        "business_topic_counts": count_values(segments, "business_topic"),
        "business_domain_counts": count_values(segments, "business_domain"),
        "tone_counts": count_values(segments, "tone"),
        "style_tag_counts": count_style_tags(segments),
        "host_style_speech_act_counts": count_values(host_style_references, "speech_act"),
        "host_style_stage_counts": count_values(host_style_references, "conversation_stage"),
        "host_style_tone_counts": count_values(host_style_references, "tone"),
        "guest_case_topic_counts": count_values(guest_cases, "business_topic"),
        "guest_case_domain_counts": count_values(guest_cases, "business_domain"),
    }


def write_json(path: Path, data: Any) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()
    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input material file does not exist: {input_path}")

    output_prefix = (
        Path(args.output_prefix).expanduser().resolve() if args.output_prefix else default_output_prefix(input_path)
    )
    segments = json.loads(input_path.read_text(encoding="utf-8"))
    host_style_references = extract_host_style_references(segments)
    guest_cases = extract_guest_cases(segments)
    stats = build_stats(segments, host_style_references, guest_cases)

    host_path = output_prefix.with_name(f"{output_prefix.name}_host_style_references.json")
    guest_path = output_prefix.with_name(f"{output_prefix.name}_guest_cases.json")
    stats_path = output_prefix.with_name(f"{output_prefix.name}_material_stats.json")

    write_json(host_path, host_style_references)
    write_json(guest_path, guest_cases)
    write_json(stats_path, stats)

    print("Material files saved:")
    print(f"- {host_path}")
    print(f"- {guest_path}")
    print(f"- {stats_path}")


if __name__ == "__main__":
    main()
