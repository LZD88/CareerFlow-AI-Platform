from llama_index.core import Document
from llama_index.core.node_parser import MarkdownNodeParser, SentenceSplitter
from llama_index.core.schema import BaseNode, TextNode


class ChunkService:
    """
    LlamaIndex 文档切片服务。

    当前策略：
    - markdown：先用 MarkdownNodeParser 按标题层级切成章节，再用 SentenceSplitter 控制 chunk 大小；
    - manual/txt/其他：直接用 SentenceSplitter 按句子/段落切。

    这样既能保留 Markdown 标题结构，又能避免单个章节过长导致 prompt 噪声过大。
    """

    def __init__(self, chunk_size: int = 800, chunk_overlap: int = 120) -> None:
        self.markdown_parser = MarkdownNodeParser(header_path_separator=" > ")
        self.sentence_splitter = SentenceSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            paragraph_separator="\n\n",
        )

    def split_nodes(self, content: str, *, source_type: str = "manual", metadata: dict | None = None) -> list[BaseNode]:
        """
        把原始文档切成 LlamaIndex Node。

        metadata 会挂到每个 node 上，后续写入 Milvus 后，检索返回时还能知道：
        - 这个 chunk 属于哪个 document；
        - 属于哪个课程/线上线下范围；
        - 如果是 Markdown，属于哪个标题路径。
        """
        text = content.lstrip("\ufeff").strip()
        if not text:
            return []

        # 代码知识库文档由生成脚本产出：一个 `## 类.方法(...)` 就是一条可诊断 chunk。
        if source_type.lower() in {"generated_java_md", "java_method_md"}:
            return self._split_generated_java_markdown(text, metadata or {})

        # 数据分析表结构文档约定：一个 Markdown 文件描述一张表。
        # 必须整文件作为一个 chunk，避免字段、索引、关联、口径被拆散后生成 SQL 时上下文不完整。
        if source_type.lower() in {"data_analysis_schema_md", "table_schema_md"}:
            return self._single_table_schema_node(text, metadata or {})

        document = Document(text=text, metadata=metadata or {})
        if source_type.lower() in {"markdown", "md"} or self._looks_like_markdown(text):
            section_nodes = self.markdown_parser.get_nodes_from_documents([document])
            return self._split_long_nodes(section_nodes)

        return self.sentence_splitter.get_nodes_from_documents([document])

    def split_text(self, content: str, *, source_type: str = "manual", metadata: dict | None = None) -> list[str]:
        """
        兼容旧调用方的文本切分方法。

        新入库流程优先使用 split_nodes，以保留 LlamaIndex metadata。
        """
        nodes = self.split_nodes(content, source_type=source_type, metadata=metadata)
        return [node.get_content().strip() for node in nodes]

    def _split_long_nodes(self, nodes: list[BaseNode]) -> list[BaseNode]:
        """
        MarkdownNodeParser 会按标题切章节。
        如果某个章节很长，再交给 SentenceSplitter 继续切成更小的 node。
        """
        if not nodes:
            return []
        return self.sentence_splitter.get_nodes_from_documents(nodes)

    def _looks_like_markdown(self, text: str) -> bool:
        """
        手动录入时用户可能没选 markdown，但正文本身是 Markdown。
        这里做一个轻量判断，让带标题的文档也能享受标题切分。
        """
        for line in text.splitlines()[:30]:
            stripped = line.strip()
            if stripped.startswith("# ") or stripped.startswith("## ") or stripped.startswith("### "):
                return True
        return False

    def _split_generated_java_markdown(self, text: str, metadata: dict) -> list[BaseNode]:
        """
        Split generated Java knowledge markdown by method sections.

        The generator emits one class document with:
        - `# ClassName` as document title;
        - each `## ClassName.method(...)` section as one diagnosis-ready chunk.

        We skip the class title itself and only index method sections. Each node
        is self-contained: path, source file, exception/log keywords, and source
        code are already inside the section.
        """
        class_title = self._extract_class_title(text)
        sections = []
        current_heading = ""
        current_lines: list[str] = []

        for line in text.splitlines():
            stripped_line = line.lstrip("\ufeff").lstrip()
            if stripped_line.startswith("## "):
                if current_lines:
                    sections.append((current_heading, "\n".join(current_lines).strip()))
                current_heading = stripped_line[3:].strip()
                current_lines = [stripped_line]
                continue
            if current_lines:
                current_lines.append(line)

        if current_lines:
            sections.append((current_heading, "\n".join(current_lines).strip()))

        nodes: list[BaseNode] = []
        for index, (heading, section_text) in enumerate(sections, start=1):
            if not section_text:
                continue
            method_name = self._extract_method_name(heading)
            node_metadata = {
                **metadata,
                "header_path": f"{class_title} > {heading}" if class_title else heading,
                "chunk_type": "java_method",
                "method_name": method_name,
                "method_heading": heading,
                "section_index": index,
            }
            nodes.append(TextNode(text=section_text, metadata=node_metadata))
        return nodes

    def _single_table_schema_node(self, text: str, metadata: dict) -> list[BaseNode]:
        table_name = self._extract_class_title(text)
        node_metadata = {
            **metadata,
            "header_path": table_name,
            "chunk_type": "data_analysis_schema",
            "table_name": table_name,
        }
        return [TextNode(text=text, metadata=node_metadata)]

    def _extract_class_title(self, text: str) -> str:
        for line in text.splitlines():
            stripped = line.strip()
            if stripped.startswith("# "):
                return stripped[2:].strip()
        return ""

    def _extract_method_name(self, heading: str) -> str:
        value = heading.strip()
        if "." in value:
            value = value.split(".", 1)[1]
        if "(" in value:
            value = value.split("(", 1)[0]
        return value.strip()
