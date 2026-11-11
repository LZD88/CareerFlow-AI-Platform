from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # 应用基础信息，主要用于接口文档和健康检查。
    app_name: str = "YQ AI Python Agent"
    app_version: str = "0.1.0"

    # 如果后面需要由 Python 主动回调 Java，可以用这个地址。
    java_backend_base_url: str = "http://127.0.0.1:8081/yq-admin"

    # 当前默认使用火山方舟的大模型；先走 OpenAI 兼容模式，后面再加知识库检索。
    model_provider: str = "mock"
    knowledge_base_name: str = "yqai"

    # 火山方舟 OpenAI 兼容配置。
    # 注意：API Key 不要写死在代码里，只从环境变量或 .env 中读取。
    ark_api_key: str = ""
    ark_base_url: str = "https://ark.cn-beijing.volces.com/api/v3"
    ark_model: str = "doubao-seed-2-0-pro-260215"

    # 火山方舟 Embedding 配置。
    # doubao-embedding-vision 走方舟向量化 API。这里默认使用多模态向量化 endpoint，
    # 即使当前只传文本，也按 [{"type": "text", "text": "..."}] 的结构提交。
    # API Key 默认可复用 ARK_API_KEY；如果配置了 EMBEDDING_API_KEY，则优先使用单独的 key。
    embedding_api_key: str = ""
    embedding_base_url: str = "https://ark.cn-beijing.volces.com/api/v3"
    embedding_path: str = "/embeddings/multimodal"
    embedding_model: str = "doubao-embedding-vision"
    embedding_dimensions: int = 1024

    # 阿里云百炼千问 OpenAI 兼容配置。
    # 学生端问答先切到这里；日志诊断仍保留原来的火山方舟配置。
    qwen_api_key: str = ""
    qwen_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    qwen_model: str = "qwen3.6-plus"
    qwen_router_model: str = "qwen3.6-flash"
    # 数据分析链路是后台交互型场景，默认优先使用 flash 降低延迟。
    # 如果后续发现 SQL 质量不够，再通过环境变量单独切回 qwen3.6-plus。
    qwen_data_analysis_sql_model: str = "qwen3.6-flash"
    qwen_data_analysis_interpret_model: str = "qwen3.6-flash"
    qwen_data_analysis_sql_max_tokens: int = 1200
    qwen_data_analysis_interpret_max_tokens: int = 1600

    # 千问重排配置。
    # 学生问答当前是纯文本知识库，因此这里默认接文本 RAG 更合适的 qwen3-rerank。
    # 链路会先从 Milvus 召回一批候选 chunk，再用 rerank 重新排序后取前 top_n 条给大模型。
    qwen_rerank_enabled: bool = True
    qwen_rerank_api_key: str = ""
    qwen_rerank_url: str = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank"
    qwen_rerank_model: str = "qwen3-rerank"
    qwen_rerank_recall_top_k: int = 12
    qwen_rerank_top_n: int = 3

    # Milvus 向量数据库配置。
    # 学生知识库和代码知识库使用不同 collection，避免课程资料和日志诊断代码互相污染。
    milvus_uri: str = "http://127.0.0.1:19530"
    milvus_token: str = ""
    milvus_db_name: str = "default"
    milvus_collection: str = "student_knowledge_chunks"
    milvus_code_collection: str = "code_knowledge_chunks"
    milvus_data_analysis_collection: str = "data_analysis_schema_chunks"
    milvus_data_analysis_metric_collection: str = "data_analysis_metric_chunks"
    milvus_vector_dim: int = 1024
    milvus_timeout_s: float = 10

    # 火山知识库服务配置。
    # 当前接的是 service chat 接口，因此需要 service_resource_id 和 Bearer apikey。
    kb_api_key: str = ""
    kb_base_url: str = "http://api-knowledgebase.mlp.cn-beijing.volces.com"
    kb_service_resource_id: str = ""

    # OpenAI 图片生成配置。
    # 需要在 OpenAI Platform 单独开通 API billing，并把 Key 放到环境变量或 .env。
    openai_api_key: str = ""
    openai_image_model: str = "gpt-image-2"

    # 面试复盘 ASR 配置。
    # 生产默认走火山/豆包录音文件识别；FunASR 保留为本地兜底。
    interview_asr_provider: str = "volcengine"
    volc_speech_api_key: str = ""
    volc_speech_app_key: str = ""
    volc_speech_access_key: str = ""
    volc_speech_resource_id: str = "volc.bigasr.auc"
    volc_speech_submit_url: str = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/submit"
    volc_speech_query_url: str = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/query"
    volc_speech_model_name: str = "bigmodel"
    volc_speech_model_version: str = "400"
    volc_speech_ssd_version: str = "200"
    volc_speech_poll_interval_s: float = 3.0
    volc_speech_max_wait_s: int = 1800
    volc_speech_request_timeout_s: int = 30
    volc_speech_max_retries: int = 3
    volc_speech_trust_env_proxy: bool = False

    # FunASR 本地兜底配置。
    # 快速转写默认用 SenseVoiceSmall；说话人分离依赖时间戳，会切到支持 timestamp 的 Paraformer 模型。
    interview_asr_model: str = "iic/SenseVoiceSmall"
    interview_asr_speaker_model: str = "iic/speech_paraformer-large-vad-punc_asr_nat-zh-cn-16k-common-vocab8404-pytorch"
    interview_asr_vad_model: str = "fsmn-vad"
    interview_asr_punc_model: str = "ct-punc"
    interview_asr_spk_model: str = "cam++"
    interview_asr_device: str = "cpu"
    interview_asr_batch_size_s: int = 60
    interview_asr_max_single_segment_time_ms: int = 30000
    interview_asr_max_segment_chars: int = 600
    interview_asr_max_merge_pause_s: float = 3.0
    interview_audio_download_timeout_s: int = 120

    # AI 模拟面试实时语音配置。
    mock_interview_realtime_token_secret: str = "change-me-mock-interview-realtime-token-secret"
    mock_interview_volc_ws_url: str = "wss://openspeech.bytedance.com/api/v3/realtime/dialogue"
    mock_interview_volc_app_id: str = ""
    mock_interview_volc_access_key: str = ""
    mock_interview_volc_resource_id: str = "volc.speech.dialog"
    mock_interview_volc_app_key: str = ""
    mock_interview_volc_model: str = "1.2.1.1"
    mock_interview_tts_speaker: str = "zh_male_yunzhou_jupiter_bigtts"
    mock_interview_tts_format: str = "pcm_s16le"
    mock_interview_tts_sample_rate: int = 24000
    mock_interview_audio_format: str = "pcm"
    mock_interview_audio_sample_rate: int = 16000
    mock_interview_audio_channel: int = 1
    mock_interview_connect_timeout_s: int = 15

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
