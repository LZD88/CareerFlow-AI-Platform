"""Label transcript content with scene, topic, tone, and style metadata.

Example:
    uv run python scripts/label_transcript_content.py \
        /Users/huangjin/Downloads/sg1_spk_role_corrected_qwen.json \
        --output /Users/huangjin/Downloads/sg1_spk_content_labeled_qwen.json
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from langchain_openai import ChatOpenAI

from app.config.settings import get_settings


SYSTEM_PROMPT = """你是商业直播连麦内容分析助手。

任务：为每一段逐字稿补充内容标签，方便后续学习“参哥式商业诊断/口播表达”。

你会看到每段的角色：
- host/参哥：主播、导师、诊断者
- guest/嘉宾：连麦者、被诊断者
- mixed/混合：可能含两个人的话

请为每个 index 返回一条标签结果，不能遗漏，不能增加不存在的 index。
输出必须是严格 JSON 对象，不要 Markdown，不要解释文字。

字段说明：
- scene: 场景，例如 "直播连麦诊断"、"案例讲解"、"成交引导"、"未知"
- speech_act: 话语动作，只能用英文小写：question, answer, diagnosis, advice, judgment, storytelling, summary, transition, sales, unknown
- conversation_stage: 对话阶段，只能用英文小写：opening, background_probe, pain_probe, diagnosis, solution, objection_handling, conversion, closing, unknown
- business_topic: 具体业务主题，中文短词，例如 "投入金额"、"亏损压力"、"产品定位"
- business_domain: 行业/方向，中文短词，例如 "健康农业"、"老板IP"、"短视频获客"、"私域成交"、"未知"
- tone: 语气，中文短词，例如 "直接追问"、"笃定判断"、"安抚鼓励"、"压迫提醒"、"平铺叙述"
- style_tags: 2 到 5 个中文标签，例如 ["金额切入", "诊断式提问"]
- is_host_style_reference: boolean。只有当该段主要是 host/参哥 的表达，且适合沉淀为参哥风格参考时才为 true。
- summary: 中文短句，最多 30 字。

重要规则：
- guest 的自述一般不作为参哥风格参考，is_host_style_reference 通常为 false。
- host 的提问、追问、判断、建议、诊断、成交话术通常可作为参哥风格参考。
- mixed 段如果内容混杂或不清楚，is_host_style_reference 设为 false。
- 不要输出长篇推理。

输出格式：
{
  "items": [
    {
      "index": 0,
      "scene": "直播连麦诊断",
      "speech_act": "question",
      "conversation_stage": "background_probe",
      "business_topic": "投入金额",
      "business_domain": "健康农业",
      "tone": "直接追问",
      "style_tags": ["金额切入", "短句追问"],
      "is_host_style_reference": true,
      "summary": "用投入金额切入诊断。"
    }
  ]
}
"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Label transcript content with an LLM.")
    parser.add_argument(
        "input",
        help="Path to role-corrected JSON, such as /Users/huangjin/Downloads/sg1_spk_role_corrected_qwen.json",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Path to write content-labeled JSON. Defaults to <input>_content_labeled.json",
    )
    parser.add_argument(
        "--model",
        help="Override qwen model name. Defaults to qwen_router_model from .env.",
    )
    parser.add_argument(
        "--max-segments",
        type=int,
        default=0,
        help="Only analyze the first N segments for testing. Default 0 means all segments.",
    )
    return parser.parse_args()


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


def build_items(segments: list[dict[str, Any]], max_segments: int) -> list[dict[str, Any]]:
    selected = segments[:max_segments] if max_segments > 0 else segments
    return [
        {
            "index": index,
            "start": segment.get("start"),
            "end": segment.get("end"),
            "speaker_role": segment.get("speaker_role"),
            "speaker_name": segment.get("speaker_name"),
            "text": segment.get("text", ""),
        }
        for index, segment in enumerate(selected)
    ]


def build_llm(model_override: str | None) -> ChatOpenAI:
    settings = get_settings()
    if not settings.qwen_api_key:
        raise ValueError("QWEN_API_KEY 未配置，无法调用千问模型")
    return ChatOpenAI(
        api_key=settings.qwen_api_key,
        base_url=settings.qwen_base_url,
        model=model_override or settings.qwen_router_model or settings.qwen_model,
        temperature=0,
        extra_body={"enable_thinking": False},
    )


def call_llm(items: list[dict[str, Any]], model_override: str | None) -> list[dict[str, Any]]:
    llm = build_llm(model_override)
    user_prompt = "请为以下直播连麦逐字稿补充内容标签：\n" + json.dumps(
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


def merge_labels(segments: list[dict[str, Any]], label_items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    label_by_index = {int(item["index"]): item for item in label_items if "index" in item}
    labeled = []
    for index, segment in enumerate(segments):
        label = label_by_index.get(index, {})
        is_host = segment.get("speaker_role") == "host"
        labeled.append(
            {
                **segment,
                "scene": label.get("scene", "未知"),
                "speech_act": label.get("speech_act", "unknown"),
                "conversation_stage": label.get("conversation_stage", "unknown"),
                "business_topic": label.get("business_topic", "未知"),
                "business_domain": label.get("business_domain", "未知"),
                "tone": label.get("tone", "未知"),
                "style_tags": label.get("style_tags", []),
                "is_host_style_reference": is_host and bool(label.get("is_host_style_reference", False)),
                "content_summary": label.get("summary", ""),
            }
        )
    return labeled


def main() -> None:
    args = parse_args()
    input_path = Path(args.input).expanduser().resolve()
    if not input_path.exists():
        raise FileNotFoundError(f"Input transcript file does not exist: {input_path}")

    output_path = (
        Path(args.output).expanduser().resolve()
        if args.output
        else input_path.with_name(f"{input_path.stem}_content_labeled.json")
    )
    segments = json.loads(input_path.read_text(encoding="utf-8"))
    items = build_items(segments, args.max_segments)
    label_items = call_llm(items, args.model)
    labeled = merge_labels(segments[: len(items)], label_items)
    output_path.write_text(json.dumps(labeled, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Content-labeled transcript saved to {output_path}")


if __name__ == "__main__":
    main()
