import json
import logging
import re
import tempfile
from pathlib import Path

import httpx
from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.interview_review import (
    InterviewQuestionExtractItem,
    InterviewQuestionExtractRequest,
    InterviewQuestionExtractResponse,
)

logger = logging.getLogger(__name__)


class InterviewQuestionExtractService:
    def __init__(self) -> None:
        self.settings = get_settings()

    def extract(self, request: InterviewQuestionExtractRequest) -> InterviewQuestionExtractResponse:
        llm = self._build_llm()
        prompt = self._build_prompt(request)
        logger.info(
            "开始提取面试题 task_id=%s role=%s transcript_len=%s",
            request.taskId,
            request.interviewRole or "",
            len(request.transcriptText or ""),
        )
        response = llm.invoke(prompt)
        content = response.content if isinstance(response.content, str) else str(response.content)
        items = self._parse_items(content)
        logger.info("面试题提取完成 task_id=%s count=%s", request.taskId, len(items))
        return InterviewQuestionExtractResponse(taskId=request.taskId, questions=items)

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_model,
                temperature=0.1,
                extra_body={"enable_thinking": False},
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0.1,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法提取面试题")

    def _build_prompt(self, request: InterviewQuestionExtractRequest) -> str:
        transcript = self._clip_text(request.transcriptText, 70000)
        resume_text = self._clip_text(request.resumeText or self._load_resume_text(request), 25000) or "未提供简历文本。"
        return f"""# 角色
你是一名技术面试辅导老师和题库建设老师，负责从真实面试转写中沉淀高价值面试问题，并为学生生成“真正正确、可练习”的标准回答。

# 硬性规则
1. 只提取“有复用价值”的问题，不要提取寒暄、确认声音、开不开摄像头等无价值问题。
2. 同一类问题只输出一次，用 normalizedQuestion 归一成标准问题。
3. 生成 standardAnswer 前，必须先在心里完成三步判断：这道题考什么、正确答案应该包含什么、学生原回答哪里对或错。
4. standardAnswer 必须是“独立标准答案”，不能只是润色、扩写或复述学生原回答。
5. 学生原回答只能作为答题质量分析和个性化衔接材料，不能作为技术事实依据；如果学生回答错误，要按正确技术事实纠正。
6. 标准答案可以使用通用技术知识、行业常识和岗位要求来补全正确答案；涉及学生个人项目时，只能使用转写或简历中出现过的信息，不能编造项目经历。
7. 如果题目依赖学生项目但材料不足，standardAnswer 要写成“可替换模板 + 需要补充的信息”，不要强行编造细节。
8. 必须评价学生当时回答是否答到点上，不能只复述对话。要指出答得好、不完整、错误、跑偏或证据不足的地方。
9. 输出必须是 JSON 数组，不要输出 Markdown，不要加解释。
10. 最多输出 15 个问题，优先选面试官真实追问、技术深挖、项目亮点相关问题。
11. studentAnswerQuality 只能取 GOOD、PARTIAL、WRONG、INSUFFICIENT、UNKNOWN：GOOD=回答较好；PARTIAL=答到一部分但不完整；WRONG=明显错误；INSUFFICIENT=回答太少或没有回答；UNKNOWN=转写无法判断。

# 标准答案生成方法
对每个问题按下面逻辑生成：
1. 先识别面试官真正考点，例如原理、方案取舍、项目落地、问题排查、系统设计或表达能力。
2. 再对照学生回答，判断学生是否覆盖核心点，是否有明显技术错误、术语错误或逻辑跳跃。
3. 最后生成 standardAnswer：以正确考点为主线，结合学生已有经历表达，但不要被学生错误回答带偏。

错误示例：学生说“Redis 用来做所有数据一致性”，不能把这句话润色进标准答案。
正确做法：说明 Redis 常用于缓存、分布式锁、限流等场景，并补充一致性风险和解决方案。

# 分析材料
## 面试基本信息
- 任务标题：{request.title or "未填写"}
- 面试岗位：{request.interviewRole or "未填写"}

## 简历文本
{resume_text}

## 面试转写
{transcript}

# 输出 JSON Schema
[
  {{
    "originalQuestion": "转写里的原始问题或根据上下文概括的问题",
    "normalizedQuestion": "归一后的标准问题，适合作为题库标题",
    "category": "Java基础/项目经验/AI/RAG/Agent/数据库/系统设计/问题排查/表达沟通/其他",
    "tags": ["标签1", "标签2"],
    "answerContext": "学生当时的回答片段摘要",
    "studentAnswerQuality": "GOOD/PARTIAL/WRONG/INSUFFICIENT/UNKNOWN",
    "studentAnswerScore": 0,
    "studentAnswerAnalysis": "基于学生原回答的质量判断：答到了什么、缺了什么、有没有错误或跑偏。80-200字",
    "improvementSuggestion": "下次这道题应该怎么补充，按 1-3 个具体动作说明",
    "standardAnswer": "独立标准答案，200-500字，先回答正确考点，再结合学生已有经历表达；不能只是改写学生原回答",
    "shortAnswer": "30-80字简短回答，必须是正确答案摘要，不是学生原话摘要",
    "followUpQuestions": ["面试官可能追问1", "面试官可能追问2"],
    "pitfalls": ["常见扣分点1", "常见扣分点2"]
  }}
]
"""

    def _parse_items(self, content: str) -> list[InterviewQuestionExtractItem]:
        raw = self._strip_json_fence(content)
        try:
            data = json.loads(raw)
        except json.JSONDecodeError as exc:
            raise ValueError(f"面试题提取结果不是合法 JSON: {exc}") from exc
        if not isinstance(data, list):
            raise ValueError("面试题提取结果必须是 JSON 数组")
        items: list[InterviewQuestionExtractItem] = []
        for item in data[:20]:
            parsed = InterviewQuestionExtractItem.model_validate(item)
            if parsed.normalizedQuestion.strip() and parsed.standardAnswer.strip():
                items.append(parsed)
        return items

    def _load_resume_text(self, request: InterviewQuestionExtractRequest) -> str:
        if not request.resumeUrl:
            return ""
        suffix = Path(request.resumeFilename or "").suffix.lower()
        if suffix == ".doc":
            return "简历文件为 .doc 格式，当前暂不支持直接解析。建议转成 PDF 或 DOCX。"
        with tempfile.TemporaryDirectory(prefix=f"interview_question_resume_{request.taskId}_") as temp_dir:
            path = Path(temp_dir) / f"resume{suffix or '.bin'}"
            with httpx.Client(timeout=60, trust_env=False) as client:
                response = client.get(request.resumeUrl)
                response.raise_for_status()
                path.write_bytes(response.content)
            if suffix == ".pdf":
                from pypdf import PdfReader

                reader = PdfReader(str(path))
                return "\n\n".join((page.extract_text() or "").strip() for page in reader.pages)
            if suffix == ".docx":
                from docx import Document

                document = Document(str(path))
                return "\n".join(p.text.strip() for p in document.paragraphs if p.text.strip())
            if suffix in {".txt", ".md"}:
                data = path.read_bytes()
                for encoding in ("utf-8", "utf-8-sig", "gb18030"):
                    try:
                        return data.decode(encoding)
                    except UnicodeDecodeError:
                        continue
                return data.decode("utf-8", errors="ignore")
        return ""

    def _strip_json_fence(self, content: str) -> str:
        text = (content or "").strip()
        match = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL | re.IGNORECASE)
        return match.group(1).strip() if match else text

    def _clip_text(self, text: str, max_chars: int) -> str:
        cleaned = (text or "").strip()
        if len(cleaned) <= max_chars:
            return cleaned
        return cleaned[:max_chars] + "\n...(内容过长，已截断)"
