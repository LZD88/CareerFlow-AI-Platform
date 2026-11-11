"""Transcribe a local audio file with FunASR.

Example:
    uv run python scripts/transcribe_audio.py \
        /Users/huangjin/Downloads/sg1.wav \
        --output /Users/huangjin/Downloads/sg1.txt
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


DEFAULT_HOTWORDS = "参哥 海参哥 抖音 私域 IP 短视频 老板 认知 成交 直播 闭环 流量 商业模式"
BREAK_CHARS = set("。！？!?；;")
SOFT_BREAK_CHARS = set("，,、")
PUNCTUATION_CHARS = BREAK_CHARS | SOFT_BREAK_CHARS | set("：:“”\"'（）()《》<>【】[]")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Transcribe audio to text with FunASR.")
    parser.add_argument("input", help="Path to the input audio file, such as /Users/huangjin/Downloads/sg1.wav")
    parser.add_argument(
        "-o",
        "--output",
        help="Path to write transcript text. Defaults to <input>.txt",
    )
    parser.add_argument(
        "--hotword",
        default=DEFAULT_HOTWORDS,
        help="Space-separated hotwords to improve recognition for domain terms.",
    )
    parser.add_argument(
        "--batch-size-s",
        type=int,
        default=300,
        help="Approximate audio seconds per batch. Reduce it if memory is tight.",
    )
    parser.add_argument(
        "--format",
        choices=["txt", "json", "srt", "all"],
        default="txt",
        help="Output format. txt writes timestamped segments by default.",
    )
    parser.add_argument(
        "--max-segment-chars",
        type=int,
        default=120,
        help="Split long utterances after this many characters when possible.",
    )
    parser.add_argument(
        "--max-merge-pause",
        type=float,
        default=1.0,
        help="Merge adjacent segments from the same speaker when the pause is no longer than this many seconds.",
    )
    parser.add_argument(
        "--no-merge-same-speaker",
        action="store_true",
        help="Keep FunASR sentence_info segments as-is instead of merging adjacent same-speaker segments.",
    )
    parser.add_argument(
        "--speaker-diarization",
        action="store_true",
        help="Enable CAM++ speaker diarization and include speaker ids in output.",
    )
    parser.add_argument(
        "--speaker-label",
        action="append",
        default=[],
        metavar="ID=NAME",
        help="Map speaker ids to names, for example --speaker-label 0=参哥 --speaker-label 1=嘉宾.",
    )
    return parser.parse_args()


def transcribe(input_path: Path, hotword: str, batch_size_s: int, speaker_diarization: bool) -> list[dict]:
    try:
        from funasr import AutoModel
    except ImportError as exc:
        raise RuntimeError(
            "FunASR or one of its runtime dependencies is missing. "
            "Run `uv sync` in YQ-AI-PY-AGENT first."
        ) from exc

    model_kwargs = {
        "model": "paraformer-zh",
        "vad_model": "fsmn-vad",
        "punc_model": "ct-punc",
        "disable_update": True,
    }
    if speaker_diarization:
        model_kwargs["spk_model"] = "cam++"

    model = AutoModel(**model_kwargs)
    return model.generate(
        input=str(input_path),
        batch_size_s=batch_size_s,
        hotword=hotword,
    )


def format_time(seconds: float) -> str:
    milliseconds = int(round(seconds * 1000))
    hours, remainder = divmod(milliseconds, 3_600_000)
    minutes, remainder = divmod(remainder, 60_000)
    secs, milliseconds = divmod(remainder, 1000)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}.{milliseconds:03d}"


def format_srt_time(seconds: float) -> str:
    return format_time(seconds).replace(".", ",")


def should_split(char: str, current_text: str, max_segment_chars: int) -> bool:
    if char in BREAK_CHARS:
        return True
    if char in SOFT_BREAK_CHARS and len(current_text) >= max_segment_chars:
        return True
    return len(current_text) >= max_segment_chars * 2


def build_segments_from_sentence_info(result: list[dict]) -> list[dict]:
    segments: list[dict] = []
    for item in result:
        for sentence in item.get("sentence_info") or []:
            text = sentence.get("text", "").strip()
            if not text:
                continue
            segments.append(
                {
                    "start": round(sentence["start"] / 1000, 3),
                    "end": round(sentence["end"] / 1000, 3),
                    "text": text,
                    "speaker": sentence.get("spk"),
                }
            )
    return segments


def build_segments(result: list[dict], max_segment_chars: int) -> list[dict]:
    sentence_segments = build_segments_from_sentence_info(result)
    if sentence_segments:
        return sentence_segments

    segments: list[dict] = []

    for item in result:
        text = item.get("text", "")
        timestamps = item.get("timestamp") or []
        timestamp_index = 0
        current_chars: list[str] = []
        current_start_ms: int | None = None
        current_end_ms: int | None = None

        def flush() -> None:
            nonlocal current_chars, current_start_ms, current_end_ms
            segment_text = "".join(current_chars).strip()
            if segment_text and current_start_ms is not None and current_end_ms is not None:
                segments.append(
                    {
                        "start": round(current_start_ms / 1000, 3),
                        "end": round(current_end_ms / 1000, 3),
                        "text": segment_text,
                        "speaker": None,
                    }
                )
            current_chars = []
            current_start_ms = None
            current_end_ms = None

        for char in text:
            current_chars.append(char)
            if char.isspace() or char in PUNCTUATION_CHARS:
                if current_end_ms is not None and should_split(char, "".join(current_chars), max_segment_chars):
                    flush()
                continue

            if timestamp_index < len(timestamps):
                start_ms, end_ms = timestamps[timestamp_index]
                timestamp_index += 1
                if current_start_ms is None:
                    current_start_ms = start_ms
                current_end_ms = end_ms

            if should_split(char, "".join(current_chars), max_segment_chars):
                flush()

        flush()

    return segments


def can_merge_segments(previous: dict, current: dict, max_segment_chars: int, max_merge_pause: float) -> bool:
    if previous.get("speaker") != current.get("speaker"):
        return False
    pause = current["start"] - previous["end"]
    if pause < 0 or pause > max_merge_pause:
        return False
    merged_length = len(previous["text"]) + len(current["text"])
    if merged_length > max_segment_chars:
        return False
    if previous["text"].endswith(("。", "！", "？", "!", "?")):
        return False
    return True


def merge_same_speaker_segments(
    segments: list[dict],
    max_segment_chars: int,
    max_merge_pause: float,
) -> list[dict]:
    if not segments:
        return []

    merged_segments = [segments[0].copy()]
    for segment in segments[1:]:
        previous = merged_segments[-1]
        if can_merge_segments(previous, segment, max_segment_chars, max_merge_pause):
            previous["end"] = segment["end"]
            previous["text"] += segment["text"]
        else:
            merged_segments.append(segment.copy())
    return merged_segments


def parse_speaker_labels(raw_labels: list[str]) -> dict[int, str]:
    labels: dict[int, str] = {}
    for raw_label in raw_labels:
        speaker_id, separator, name = raw_label.partition("=")
        if not separator:
            raise ValueError(f"Invalid speaker label: {raw_label}. Expected format: ID=NAME")
        labels[int(speaker_id)] = name
    return labels


def speaker_name(segment: dict, labels: dict[int, str]) -> str | None:
    speaker = segment.get("speaker")
    if speaker is None:
        return None
    return labels.get(int(speaker), f"说话人{speaker}")


def render_txt(segments: list[dict], labels: dict[int, str]) -> str:
    lines = []
    for segment in segments:
        speaker = speaker_name(segment, labels)
        speaker_prefix = f"{speaker}: " if speaker else ""
        lines.append(f"[{format_time(segment['start'])} --> {format_time(segment['end'])}] {speaker_prefix}{segment['text']}")
    return "\n".join(lines)


def render_srt(segments: list[dict], labels: dict[int, str]) -> str:
    blocks = []
    for index, segment in enumerate(segments, start=1):
        speaker = speaker_name(segment, labels)
        text = f"{speaker}: {segment['text']}" if speaker else segment["text"]
        blocks.append(
            "\n".join(
                [
                    str(index),
                    f"{format_srt_time(segment['start'])} --> {format_srt_time(segment['end'])}",
                    text,
                ]
            )
        )
    return "\n\n".join(blocks)


def write_output(output_path: Path, output_format: str, segments: list[dict], labels: dict[int, str]) -> list[Path]:
    written_paths: list[Path] = []

    def path_for(suffix: str) -> Path:
        return output_path if output_format != "all" else output_path.with_suffix(suffix)

    if output_format in {"txt", "all"}:
        txt_path = path_for(".txt")
        txt_path.write_text(render_txt(segments, labels) + "\n", encoding="utf-8")
        written_paths.append(txt_path)

    if output_format in {"json", "all"}:
        json_path = path_for(".json")
        json_path.write_text(json.dumps(segments, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        written_paths.append(json_path)

    if output_format in {"srt", "all"}:
        srt_path = path_for(".srt")
        srt_path.write_text(render_srt(segments, labels) + "\n", encoding="utf-8")
        written_paths.append(srt_path)

    return written_paths


def main() -> None:
    args = parse_args()
    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input audio file does not exist: {input_path}")

    default_suffix = ".txt" if args.format == "all" else f".{args.format}"
    output_path = Path(args.output).expanduser().resolve() if args.output else input_path.with_suffix(default_suffix)
    labels = parse_speaker_labels(args.speaker_label)
    result = transcribe(input_path, args.hotword, args.batch_size_s, args.speaker_diarization)
    segments = build_segments(result, args.max_segment_chars)
    if not args.no_merge_same_speaker:
        segments = merge_same_speaker_segments(segments, args.max_segment_chars, args.max_merge_pause)
    written_paths = write_output(output_path, args.format, segments, labels)
    print("Transcript saved:")
    for path in written_paths:
        print(f"- {path}")


if __name__ == "__main__":
    main()
