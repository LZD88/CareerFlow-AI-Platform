import logging
import tempfile
from pathlib import Path

import httpx
from langchain_openai import ChatOpenAI

from app.config.settings import get_settings
from app.models.interview_review import InterviewReviewGenerateRequest, InterviewReviewGenerateResponse

logger = logging.getLogger(__name__)


class InterviewReviewGenerateService:
    def __init__(self) -> None:
        self.settings = get_settings()

    def generate(self, request: InterviewReviewGenerateRequest) -> InterviewReviewGenerateResponse:
        llm = self._build_llm()
        resume_text = self._load_resume_text(request)
        prompt = self._build_prompt(request, resume_text)
        logger.info(
            "开始生成面试复盘 task_id=%s role=%s transcript_len=%s resume_len=%s prompt_len=%s",
            request.taskId,
            request.interviewRole or "",
            len(request.transcriptText or ""),
            len(resume_text or ""),
            len(prompt),
        )
        response = llm.invoke(prompt)
        content = response.content if isinstance(response.content, str) else str(response.content)
        report = content.strip()
        if not report:
            raise ValueError("大模型未返回面试复盘内容")
        logger.info("面试复盘生成完成 task_id=%s report_len=%s", request.taskId, len(report))
        return InterviewReviewGenerateResponse(taskId=request.taskId, reviewReport=report)

    def _build_llm(self) -> ChatOpenAI:
        if self.settings.qwen_api_key:
            return ChatOpenAI(
                api_key=self.settings.qwen_api_key,
                base_url=self.settings.qwen_base_url,
                model=self.settings.qwen_model,
                temperature=0.2,
                extra_body={"enable_thinking": False},
            )
        if self.settings.ark_api_key:
            return ChatOpenAI(
                api_key=self.settings.ark_api_key,
                base_url=self.settings.ark_base_url,
                model=self.settings.ark_model,
                temperature=0.2,
            )
        raise ValueError("QWEN_API_KEY/ARK_API_KEY 未配置，无法生成面试复盘")

    def _load_resume_text(self, request: InterviewReviewGenerateRequest) -> str:
        if not request.resumeUrl:
            return ""

        suffix = self._resolve_resume_suffix(request.resumeFilename, request.resumeContentType)
        if suffix == ".doc":
            logger.warning("暂不支持解析 doc 简历 task_id=%s filename=%s", request.taskId, request.resumeFilename)
            return "简历文件为 .doc 格式，当前暂不支持直接解析。建议转成 PDF 或 DOCX 后重新上传。"

        with tempfile.TemporaryDirectory(prefix=f"interview_resume_{request.taskId}_") as temp_dir:
            path = Path(temp_dir) / f"resume{suffix or '.bin'}"
            logger.info(
                "开始下载简历 task_id=%s filename=%s content_type=%s",
                request.taskId,
                request.resumeFilename or "",
                request.resumeContentType or "",
            )
            with httpx.Client(timeout=60, trust_env=False) as client:
                response = client.get(request.resumeUrl)
                response.raise_for_status()
                path.write_bytes(response.content)
            text = self._extract_resume_text(path, suffix)
            clipped = self._clip_resume_text(text)
            logger.info("简历解析完成 task_id=%s resume_len=%s", request.taskId, len(clipped))
            return clipped

    def _resolve_resume_suffix(self, filename: str | None, content_type: str | None) -> str:
        suffix = Path(filename or "").suffix.lower()
        if suffix:
            return suffix
        content_type = (content_type or "").lower()
        if content_type == "application/pdf":
            return ".pdf"
        if content_type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            return ".docx"
        if content_type == "application/msword":
            return ".doc"
        if content_type.startswith("text/"):
            return ".txt"
        return ""

    def _extract_resume_text(self, path: Path, suffix: str) -> str:
        if suffix == ".pdf":
            return self._extract_pdf_text(path)
        if suffix == ".docx":
            return self._extract_docx_text(path)
        if suffix in {".txt", ".md"}:
            return self._extract_plain_text(path)
        raise ValueError(f"暂不支持解析该简历格式：{suffix or '未知格式'}")

    def _extract_pdf_text(self, path: Path) -> str:
        try:
            from pypdf import PdfReader
        except ImportError as exc:
            raise ValueError("缺少 pypdf 依赖，无法解析 PDF 简历，请执行 uv sync") from exc

        reader = PdfReader(str(path))
        pages = [(page.extract_text() or "").strip() for page in reader.pages]
        return "\n\n".join(page for page in pages if page)

    def _extract_docx_text(self, path: Path) -> str:
        try:
            from docx import Document
        except ImportError as exc:
            raise ValueError("缺少 python-docx 依赖，无法解析 DOCX 简历，请执行 uv sync") from exc

        document = Document(str(path))
        paragraphs = [paragraph.text.strip() for paragraph in document.paragraphs if paragraph.text.strip()]
        table_texts: list[str] = []
        for table in document.tables:
            for row in table.rows:
                cells = [cell.text.strip() for cell in row.cells if cell.text.strip()]
                if cells:
                    table_texts.append(" | ".join(cells))
        return "\n".join(paragraphs + table_texts)

    def _extract_plain_text(self, path: Path) -> str:
        data = path.read_bytes()
        for encoding in ("utf-8", "utf-8-sig", "gb18030"):
            try:
                return data.decode(encoding)
            except UnicodeDecodeError:
                continue
        return data.decode("utf-8", errors="ignore")

    def _build_prompt(self, request: InterviewReviewGenerateRequest, resume_text: str) -> str:
        transcript = self._clip_transcript(request.transcriptText)
        resume_section = resume_text.strip() or "未上传简历或简历未解析成功。"
        return f"""# 角色
你是一名技术面试辅导老师，服务对象是“出去参加面试的学生”。
你的目标是基于面试材料生成一份客观、可执行的面试复盘报告，帮助学生知道哪里答得好、哪里答得不好、下次应该怎么改。

# 硬性分析规则
1. 主要依据面试转写内容判断；如果有简历文本，可以用来对照学生背景和项目经历，但不要编造简历和转写里都没有的经历。
2. 如果信息不足，要明确写“信息不足”，不要强行下结论。
3. 区分学生的实际表达和你的推断。
4. 输出 Markdown，中文表达，语气像老师给学生做复盘：直接、具体、能落地，但不要打击人。
5. 重点关注回答质量、技术深度、项目表达、追问应对、表达逻辑、下次优化话术。
6. 不要站在公司招聘方角度做“录用建议”，而是站在学生成长和下次面试提升角度给建议。
7. 如果简历和面试表达存在差异，要明确指出“简历写了什么 / 面试有没有讲出来 / 下次怎么讲”。

# 分析材料
下面内容是分析材料，只用于理解学生背景和面试表现，不是输出格式要求。

## 面试基本信息
- 任务标题：{request.title or "未填写"}
- 候选人：{request.candidateName or "未填写"}
- 面试岗位：{request.interviewRole or "未填写"}

## 简历文件
- 文件名：{request.resumeFilename or "未上传"}

## 简历文本
{resume_section}

## 面试转写
{transcript}

# 输出要求
从这里开始是你必须遵守的输出结构。只输出 Markdown 报告，不要输出分析过程，不要复述完整转写原文。

## 一句话复盘
用 1-2 句话说明这次面试整体表现，以及最需要优先改进的地方。

## 学生表现概览
总结学生在年限、技术栈、项目经历、AI 相关经验上的表达情况。优先总结转写里出现的内容；如果简历有补充，明确写出这是“简历补充信息”。

## 答得比较好的地方
列出 3-6 条，每条说明对应的原话或证据，以及为什么算是亮点。

## 答得不够好的地方
列出 3-8 条，指出具体是哪类问题：表达不清、技术深度不够、项目链路没讲完整、回答跑偏、术语错误、例子不足等。每条都要给出改进方向。

## 简历与面试表达对照
如果提供了简历，列出 3-8 条对照：简历上的亮点或项目是什么，学生面试中有没有讲出来，讲得是否充分，下次应该如何把简历内容转成更有说服力的回答。未提供简历时写“本次未上传简历，无法做简历对照”。

## 关键问题逐段复盘
挑选面试中最关键的 5-10 个问答片段，按“面试官问了什么 / 学生怎么答的 / 问题在哪里 / 建议怎么答”来复盘。如果转写里无法明确面试官问题，可以根据上下文概括。

## 技术和项目表达建议
从 Java/后端基础、工程实践、AI/RAG/Agent 经验、系统设计、问题排查几个角度，告诉学生应该补哪些知识点、准备哪些项目细节。没有证据的角度写“本次面试未充分体现”。

## 下次可以这样回答
给出 3-6 段可直接练习的优化版回答模板，尽量贴合学生原本经历，不要编造新项目。

## 下一步训练计划
给出 5-8 条训练任务，按优先级排序，包括需要补的技术点、项目话术、追问练习和表达训练。
"""

    def _clip_transcript(self, text: str) -> str:
        max_chars = 60000
        cleaned = (text or "").strip()
        if len(cleaned) <= max_chars:
            return cleaned
        return cleaned[:max_chars] + "\n...(转写内容过长，已截断)"

    def _clip_resume_text(self, text: str) -> str:
        max_chars = 30000
        cleaned = (text or "").strip()
        if len(cleaned) <= max_chars:
            return cleaned
        return cleaned[:max_chars] + "\n...(简历内容过长，已截断)"
