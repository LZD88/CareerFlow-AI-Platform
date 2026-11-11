import asyncio
import json
import logging
from collections.abc import AsyncIterator

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from llama_index.core.schema import NodeWithScore

from app.config.settings import get_settings
from app.models.student_chat import StudentChatStreamRequest, StudentChatSummarizeRequest
from app.services.knowledge_base.llama_index_retriever import LlamaIndexKnowledgeRetriever
from app.services.knowledge_base.qwen_rerank_service import QwenRerankService
from app.services.student_chat.rag_router_service import RagRouteDecision, RagRouterService

logger = logging.getLogger(__name__)


class StudentChatService:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.rag_router_service = RagRouterService()
        self.rerank_service = QwenRerankService()

    async def stream_chat(self, request: StudentChatStreamRequest) -> AsyncIterator[str]:
        """
        学生端问答的 Python 流式出口。

        当前阶段已经接入真实 LLM stream：
        1. Java 把当前问题、会话摘要和最近消息传给 Python；
        2. Python 组装适合学生端问答的 messages；
        3. Python 调 OpenAI-compatible ChatOpenAI.astream；
        4. 每收到一个模型 delta，就返回一行 TOKEN NDJSON 给 Java；
        5. Java 继续转发给前端并负责最终落库。
        """
        # Java 侧按行读取，因此这里使用 NDJSON：每一行都是一个完整 JSON。
        # 这样比让 Java 再解析一层 SSE 更简单，也方便后续 SOURCE/DONE/ERROR 扩展。
        yield self._event_line({"type": "START", "message": "Python Agent 已开始处理"})

        if not self.settings.qwen_api_key:
            yield self._event_line({"type": "ERROR", "message": "QWEN_API_KEY 未配置，无法调用千问学生问答模型"})
            return

        output_text = ""
        try:
            route_decision = await asyncio.to_thread(self.rag_router_service.decide, request)
            retrieved_nodes = await asyncio.to_thread(self._retrieve_contexts, request, route_decision)
            async for delta in self._stream_llm(request, retrieved_nodes, route_decision):
                if not delta:
                    continue
                output_text += delta
                yield self._event_line({"type": "TOKEN", "delta": delta})
        except Exception as ex:
            logger.exception("学生问答模型流式调用失败: request_id=%s", request.requestId)
            yield self._event_line({"type": "ERROR", "message": self._build_error_message(ex)})
            return

        if not output_text:
            logger.warning(
                "学生问答模型流式调用未返回任何文本: request_id=%s, model=%s",
                request.requestId,
                self.settings.qwen_model,
            )
            yield self._event_line({"type": "ERROR", "message": "模型没有返回内容，请检查模型是否支持流式输出"})
            return

        yield self._event_line(
            {
                "type": "DONE",
                "finishReason": "stop",
                "usage": {
                    "inputTokens": self._estimate_tokens(request.question.content),
                    "outputTokens": self._estimate_tokens(output_text),
                },
            }
        )

    def summarize(self, request: StudentChatSummarizeRequest) -> dict:
        """
        生成会话长期摘要。

        这个接口不参与前端流式回答主链路。Java 在回答完成后异步触发，
        用于把较早的未压缩消息合并进 chat_session.summary。
        """
        if not request.messages:
            return {
                "ok": True,
                "summary": request.previousSummary.strip(),
                "summaryTokenCount": self._estimate_tokens(request.previousSummary),
                "compressedUntilMessageId": None,
            }
        if not self.settings.qwen_api_key:
            raise ValueError("QWEN_API_KEY 未配置，无法生成会话摘要")

        # 摘要使用更轻的小模型，且非流式调用；主回答已经结束，这里只负责后台压缩记忆。
        llm = ChatOpenAI(
            api_key=self.settings.qwen_api_key,
            base_url=self.settings.qwen_base_url,
            model=self.settings.qwen_router_model,
            temperature=0,
            max_tokens=max(256, request.maxSummaryTokens),
            streaming=False,
            extra_body={"enable_thinking": False},
            timeout=45,
        )
        response = llm.invoke(self._build_summarize_messages(request))
        summary = self._extract_content_text(getattr(response, "content", "")).strip()
        if not summary:
            raise ValueError("模型未返回会话摘要")

        # Java 会用这个 ID 标记哪些消息已经进入 summary。
        compressed_until_message_id = request.messages[-1].id
        return {
            "ok": True,
            "summary": summary,
            "summaryTokenCount": self._estimate_tokens(summary),
            "compressedUntilMessageId": compressed_until_message_id,
        }

    async def _stream_llm(
        self,
        request: StudentChatStreamRequest,
        retrieved_nodes: list[NodeWithScore],
        route_decision: RagRouteDecision,
    ) -> AsyncIterator[str]:
        """
        调用真实大模型并转成纯文本 delta。

        这里使用 LangChain 的 ChatOpenAI.astream。
        千问百炼提供 OpenAI-compatible 接口，因此只要传入 qwen 的 api_key/base_url/model 即可。

        注意：LangChain 会把模型流包装成 AIMessageChunk。
        很多 OpenAI-compatible 网关第一段会返回 content='' 的空元数据块，这是正常现象。
        所以这里会跳过空 chunk，并记录 chunk_count/text_chunk_count 方便排查。
        """
        llm = ChatOpenAI(
            api_key=self.settings.qwen_api_key,
            base_url=self.settings.qwen_base_url,
            model=self.settings.qwen_model,
            temperature=0.3,
            max_tokens=max(256, request.generation.maxTokens or 2000),
            streaming=True,
            extra_body={"enable_thinking": False},
            timeout=60,
        )

        logger.info(
            "开始学生问答模型流式调用: request_id=%s, session_id=%s, model=%s",
            request.requestId,
            request.sessionId,
            self.settings.qwen_model,
        )

        chunk_count = 0
        text_chunk_count = 0
        async for chunk in llm.astream(self._build_messages(request, retrieved_nodes, route_decision)):
            chunk_count += 1
            delta = self._extract_langchain_delta(chunk)
            if not delta:
                logger.debug(
                    "学生问答收到空 LangChain chunk: request_id=%s, chunk_index=%s, metadata=%s",
                    request.requestId,
                    chunk_count,
                    getattr(chunk, "response_metadata", None),
                )
                continue

            text_chunk_count += 1
            yield delta

        logger.info(
            "学生问答模型流式调用完成: request_id=%s, chunk_count=%s, text_chunk_count=%s",
            request.requestId,
            chunk_count,
            text_chunk_count,
        )

    def _build_messages(
        self,
        request: StudentChatStreamRequest,
        retrieved_nodes: list[NodeWithScore],
        route_decision: RagRouteDecision,
    ) -> list[BaseMessage]:
        """
        构造学生问答 prompt。

        当前已接入 LlamaIndex Retriever + Milvus RAG：
        - 如果检索到课程资料，优先基于资料回答；
        - 如果资料不足，明确说明资料不足；
        - 不要编造平台规则、课程要求或个人数据。
        """
        messages: list[BaseMessage] = [SystemMessage(content=self._build_system_prompt(route_decision))]

        rag_context = self._build_rag_context(retrieved_nodes)
        if rag_context:
            messages.append(
                SystemMessage(
                    content=(
                        "以下是从知识库检索到的参考资料。回答时优先依据这些资料；"
                        "如果资料之间有冲突或不足，请如实说明。"
                        "回答给学生时只能展示“可展示来源”，不要展示 document_id、chunk_id、score 等内部标识。\n\n"
                        f"{rag_context}"
                    )
                )
            )

        if request.memory.summary:
            messages.append(
                SystemMessage(
                    content=(
                        "以下是本会话的长期摘要，供理解多轮上下文使用：\n"
                        f"{request.memory.summary}"
                    )
                )
            )

        for item in request.memory.recentMessages[-8:]:
            content = item.content.strip()
            if not content:
                continue
            if item.role == "assistant":
                messages.append(AIMessage(content=content))
            elif item.role == "user":
                messages.append(HumanMessage(content=content))

        messages.append(HumanMessage(content=request.question.content))
        return messages

    def _build_summarize_messages(self, request: StudentChatSummarizeRequest) -> list[BaseMessage]:
        previous_summary = request.previousSummary.strip() or "无"
        lines: list[str] = []
        for item in request.messages:
            role = "学生" if item.role == "user" else "AI助手" if item.role == "assistant" else item.role
            content = item.content.strip()
            if content:
                lines.append(f"{role}: {content}")

        return [
            SystemMessage(
                content=(
                    "你是职业培训与就业服务平台学生问答系统的会话记忆压缩器。"
                    "请把历史对话压缩成后续回答可用的长期记忆。\n"
                    "要求：\n"
                    "1. 保留学生明确表达的学习目标、困惑点、课程背景和偏好。\n"
                    "2. 保留已经确认过的事实、作业要求、平台规则和重要结论。\n"
                    "3. 不要保留无意义寒暄，不要逐字复述。\n"
                    "4. 不要编造未出现的信息。\n"
                    "5. 使用简洁中文，按要点组织。"
                )
            ),
            HumanMessage(
                content=(
                    f"已有长期摘要：\n{previous_summary}\n\n"
                    "本次需要合并进长期摘要的历史消息：\n"
                    f"{chr(10).join(lines)}\n\n"
                    f"请输出更新后的长期摘要，控制在约 {request.maxSummaryTokens} token 内。"
                )
            ),
        ]

    def _retrieve_contexts(
        self,
        request: StudentChatStreamRequest,
        route_decision: RagRouteDecision,
    ) -> list[NodeWithScore]:
        """
        使用 LlamaIndex Retriever 检索知识节点。

        这里不再把 NodeWithScore 转回项目自己的 dict。
        从这一层开始，学生问答服务直接拿 LlamaIndex 的节点对象继续往下组装 prompt。
        这样代码含义更清楚：Retriever 返回的是“带分数的知识节点”，不是普通业务 DTO。
        """
        question = request.question.content.strip()
        if not question:
            return []

        logger.info(
            "学生问答 RAG Router 判断完成: request_id=%s, need_rag=%s, source=%s, confidence=%s, reason=%s",
            request.requestId,
            route_decision.need_rag,
            route_decision.source,
            route_decision.confidence,
            route_decision.reason,
        )
        if not route_decision.need_rag:
            return []

        try:
            recall_top_k = self._get_recall_top_k()
            retriever = LlamaIndexKnowledgeRetriever(
                course_id=request.student.courseId,
                study_mode=request.student.studyMode,
                top_k=recall_top_k,
            )
            nodes = retriever.retrieve(question)
            nodes = self._rerank_nodes(question, nodes)
            logger.info(
                "学生问答 LlamaIndex RAG 检索完成: request_id=%s, course_id=%s, study_mode=%s, result_count=%s, rerank_enabled=%s",
                request.requestId,
                request.student.courseId,
                request.student.studyMode,
                len(nodes),
                self.settings.qwen_rerank_enabled,
            )
            return nodes
        except Exception:
            logger.exception("学生问答 RAG 检索失败，将降级为普通大模型回答: request_id=%s", request.requestId)
            return []

    def _get_recall_top_k(self) -> int:
        """
        启用重排时，先多召回一点候选资料，再交给 reranker 精排。

        如果不做这一步，只召回 5 条再重排，收益通常不明显，因为真正相关的 chunk
        可能压根没进入候选集合。
        """
        if self.settings.qwen_rerank_enabled:
            return max(self.settings.qwen_rerank_top_n, self.settings.qwen_rerank_recall_top_k)
        return 5

    def _rerank_nodes(self, question: str, nodes: list[NodeWithScore]) -> list[NodeWithScore]:
        """
        对向量召回结果做二次重排。

        当前知识库是纯文本，因此使用 qwen3-rerank 而不是 qwen3-vl-rerank。
        如果重排失败，会自动回退为原始向量排序，不影响主回答链路。
        """
        if not nodes:
            return []
        if not self.settings.qwen_rerank_enabled:
            return nodes[:5]
        return self.rerank_service.rerank(
            query=question,
            nodes=nodes,
            top_n=self.settings.qwen_rerank_top_n,
        )

    def _build_system_prompt(self, route_decision: RagRouteDecision) -> str:
        base = (
            "你是职业培训与就业服务平台的 AI 学习助手，面向正在学习的学员回答问题。\n"
            "回答要求：\n"
            "1. 使用简洁、耐心、适合学生理解的中文。\n"
            "2. 优先使用 Markdown；步骤说明用有序列表，规则/注意事项用要点列表。\n"
            "3. 简单问题直接回答；复杂问题按“结论 / 说明 / 建议 / 参考来源”组织，不要为了格式而啰嗦。\n"
            "4. 不要编造平台规则、课程要求、作业要求、缴费/退费政策或学生个人数据。\n"
            "5. 涉及缴费、退费、合同、账号、个人信息等高风险问题时，如果资料不足或需要人工确认，"
            "请明确建议联系班主任、招生老师或教务。\n"
            "6. 不要向学生展示内部字段或检索细节，例如 document_id、chunk_id、score、向量、prompt。\n"
            "7. 如果无法根据资料确认，请直接说明“当前资料不足以确认”，并给出下一步建议。\n"
            "8. 不要说“根据资料1/资料2”这种机械表达，可以自然地说“从当前资料看”。\n"
        )
        if route_decision.need_rag:
            return (
                base
                + "本次问题已判断需要查询知识库。请优先基于检索到的课程或机构资料回答。\n"
                + "如果资料能回答，直接给出结论和必要说明；如果资料不足，不要猜测。\n"
                + "回答末尾可以添加“参考来源”，只展示资料标题和章节，不展示内部 ID。"
            )
        return (
            base
            + "本次问题已判断为通用问题或不依赖内部知识库的问题。\n"
            + "请直接基于通用知识回答；不要假装查询过内部课程资料，也不要引用不存在的内部来源。"
        )

    def _build_rag_context(self, nodes: list[NodeWithScore]) -> str:
        """
        把 LlamaIndex 检索节点组装成给大模型看的参考资料文本。

        注意：这里仍然只是“把节点内容放进 prompt”。
        下一步如果继续往 LlamaIndex 深走，可以把这段替换成 ResponseSynthesizer，
        让 LlamaIndex 负责根据 nodes 合成回答上下文。
        """
        lines: list[str] = []
        for index, node_with_score in enumerate(nodes[:5], start=1):
            node = node_with_score.node
            metadata = node.metadata or {}
            content = node.get_content().strip()
            if not content:
                continue
            title = str(metadata.get("title") or "未命名资料").strip()
            header_path = self._normalize_header_path(metadata.get("header_path"))
            document_id = str(metadata.get("document_id") or "").strip()
            chunk_id = str(metadata.get("chunk_id") or "").strip()
            score = node_with_score.score
            if len(content) > 1200:
                content = content[:1200] + "...(truncated)"
            display_source = title if not header_path else f"{title} / {header_path}"
            source_lines = [
                f"可展示来源: {display_source}",
                f"内部标识: document_id={document_id}, chunk_id={chunk_id}, score={score}",
            ]
            lines.append(
                "\n".join(
                    [
                        f"[资料{index}]",
                        *source_lines,
                        f"内容:\n{content}",
                    ]
                )
            )
        return "\n\n".join(lines)

    def _normalize_header_path(self, value: object) -> str:
        """
        LlamaIndex MarkdownNodeParser 会把标题路径放在 header_path 里。

        原始值通常类似 ` > 缴费说明 > Java 线下班 > `。
        给大模型看时去掉首尾分隔符，保留中间层级，方便模型知道 chunk 属于哪一节。
        """
        if not isinstance(value, str):
            return ""
        return value.strip().strip(">").strip()

    def _extract_langchain_delta(self, chunk: object) -> str:
        """
        从 LangChain 的 AIMessageChunk 中提取文本增量。

        常见情况：
        - chunk.content 是 str：直接返回
        - chunk.content 是 list：兼容多模态/分块格式，提取 text/content 字段
        - chunk.content 为空：尝试 chunk.text，兼容旧版 chunk.text()
        """
        content = getattr(chunk, "content", None)
        text = self._extract_content_text(content)
        if text:
            return text

        text_fn = getattr(chunk, "text", None)
        if isinstance(text_fn, str):
            return text_fn
        if callable(text_fn):
            try:
                value = text_fn()
                if isinstance(value, str):
                    return value
            except Exception:
                logger.debug("读取 LangChain chunk.text() 失败", exc_info=True)
        return ""

    def _extract_content_text(self, content: object) -> str:
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                elif isinstance(item, dict):
                    text = item.get("text") or item.get("content")
                    if isinstance(text, str):
                        parts.append(text)
            return "".join(parts)
        if content is None:
            return ""
        return str(content)

    def _build_error_message(self, ex: Exception) -> str:
        """
        给 Java/前端一个可定位的错误信息。

        之前只返回“模型调用失败”，排查时看不出是网络问题、API Key 问题还是模型未开通。
        这里保留异常里的关键业务错误，但限制长度，避免把过长响应直接塞到前端。
        """
        raw = str(ex).strip()
        if not raw:
            return "模型调用失败，请稍后再试"
        if "ModelNotOpen" in raw:
            return f"模型未开通或模型名配置错误：{self.settings.qwen_model}"
        if "Unauthorized" in raw or "401" in raw:
            return "模型 API Key 无效或无权限"
        if "timeout" in raw.lower():
            return "模型调用超时，请稍后再试"
        return raw[:300]

    def _build_mock_answer(self, request: StudentChatStreamRequest) -> str:
        recent_count = len(request.memory.recentMessages)
        return (
            "我已经通过 Python Agent 收到你的问题：\n\n"
            f"> {request.question.content}\n\n"
            "当前阶段已经打通 Java 到 Python 的流式链路。"
            f"这次请求携带了 {recent_count} 条最近上下文。"
            "下一步接入 LlamaIndex 和 Milvus 后，我会先检索课程知识库，再基于资料回答。"
        )

    def _split_chunks(self, value: str, chunk_size: int) -> list[str]:
        return [value[index : index + chunk_size] for index in range(0, len(value), chunk_size)]

    def _estimate_tokens(self, value: str) -> int:
        return max(1, len(value.strip()) // 2) if value.strip() else 0

    def _event_line(self, event: dict) -> str:
        return json.dumps(event, ensure_ascii=False) + "\n"
