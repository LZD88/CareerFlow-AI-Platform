package cn.yq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "yq.python.agent")
public class PythonAgentProperties {

    /**
     * 是否启用 Python Agent 调用。
     */
    private Boolean enabled = Boolean.TRUE;

    /**
     * Python Agent 基础地址，例如 http://127.0.0.1:9000
     */
    private String baseUrl = "http://127.0.0.1:9000";

    /**
     * 诊断接口路径。
     */
    private String diagnosePath = "/api/diagnose-log";

    /**
     * 学生问答流式接口路径。
     */
    private String studentChatStreamPath = "/api/student-chat/stream";

    /**
     * 学生问答会话摘要接口路径。
     */
    private String studentChatSummarizePath = "/api/student-chat/summarize";

    /**
     * 知识库文本入库接口路径。
     */
    private String knowledgeIngestTextPath = "/api/knowledge-base/ingest-text";

    /**
     * 知识库文档 chunk 查询接口路径。
     */
    private String knowledgeDocumentChunksPath = "/api/knowledge-base/document-chunks";

    /**
     * 知识库文档 chunk 删除接口路径。
     */
    private String knowledgeDeleteDocumentChunksPath = "/api/knowledge-base/delete-document-chunks";

    /**
     * 数据分析选表规划接口路径。
     */
    private String dataAnalysisPlanPath = "/api/data-analysis/plan";

    /**
     * 数据分析 SQL 生成接口路径。
     */
    private String dataAnalysisGenerateSqlPath = "/api/data-analysis/generate-sql";

    /**
     * 数据分析结果解释接口路径。
     */
    private String dataAnalysisInterpretResultPath = "/api/data-analysis/interpret-result";

    /**
     * 自媒体选题生成接口路径。
     */
    private String mediaAgentTopicsPath = "/api/media-agent/topics";

    /**
     * 自媒体文字草稿生成接口路径。
     */
    private String mediaAgentTextDraftPath = "/api/media-agent/drafts/generate-text";

    /**
     * 面试复盘音频转写接口路径。
     */
    private String interviewReviewTranscribePath = "/api/interview-review/transcribe";

    /**
     * 面试复盘报告生成接口路径。
     */
    private String interviewReviewGeneratePath = "/api/interview-review/generate-review";

    /**
     * 面试问题题库提取接口路径。
     */
    private String interviewQuestionExtractPath = "/api/interview-review/extract-questions";

    /**
     * 数据分析 SQL 生成超时时间，单位毫秒。
     *
     * <p>该接口会走大模型和 Milvus 表结构检索，耗时通常比普通 Python Agent 调用更长。</p>
     */
    private Integer dataAnalysisGenerateSqlTimeoutMs = 180000;

    /**
     * 数据分析结果解释超时时间，单位毫秒。
     *
     * <p>查询结果较多时，大模型总结也可能慢于普通接口。</p>
     */
    private Integer dataAnalysisInterpretResultTimeoutMs = 180000;

    /**
     * 自媒体选题生成超时时间，单位毫秒。
     */
    private Integer mediaAgentTopicsTimeoutMs = 120000;

    /**
     * 自媒体文字草稿生成超时时间，单位毫秒。
     */
    private Integer mediaAgentTextDraftTimeoutMs = 180000;

    /**
     * 面试复盘音频转写超时时间，单位毫秒。
     */
    private Integer interviewReviewTranscribeTimeoutMs = 1800000;

    /**
     * 面试复盘报告生成超时时间，单位毫秒。
     */
    private Integer interviewReviewGenerateTimeoutMs = 300000;

    /**
     * 面试问题题库提取超时时间，单位毫秒。
     */
    private Integer interviewQuestionExtractTimeoutMs = 300000;

    /**
     * 调用超时时间，单位毫秒。
     */
    private Integer timeoutMs = 15000;
}
