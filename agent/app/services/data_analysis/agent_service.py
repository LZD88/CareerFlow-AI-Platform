import logging

from app.models.data_analysis import (
    DataAnalysisGenerateSqlRequest,
    DataAnalysisGenerateSqlResponse,
    DataAnalysisMetricItem,
    DataAnalysisMetricSearchRequest,
    DataAnalysisPlanRequest,
    DataAnalysisSchemaRequest,
)
from app.services.data_analysis.metric_service import DataAnalysisMetricService
from app.services.data_analysis.plan_service import DataAnalysisPlanService
from app.services.data_analysis.schema_service import DataAnalysisSchemaService
from app.services.data_analysis.clarification_service import DataAnalysisClarificationService
from app.services.data_analysis.knowledge_gap_service import DataAnalysisKnowledgeGapService
from app.services.data_analysis.sql_generation_service import DataAnalysisSqlGenerationService
from app.services.data_analysis.sql_guard import DataAnalysisSqlGuard

logger = logging.getLogger(__name__)


class DataAnalysisAgentService:
    """数据分析 Agent 编排服务：只生成 SQL，不执行数据库查询。"""

    def __init__(self) -> None:
        self.plan_service = DataAnalysisPlanService()
        self.metric_service = DataAnalysisMetricService()
        self.schema_service = DataAnalysisSchemaService()
        self.clarification_service = DataAnalysisClarificationService()
        self.knowledge_gap_service = DataAnalysisKnowledgeGapService()
        self.sql_generation_service = DataAnalysisSqlGenerationService()
        self.sql_guard = DataAnalysisSqlGuard()

    def generate_sql(self, request: DataAnalysisGenerateSqlRequest) -> DataAnalysisGenerateSqlResponse:
        needs_clarification, clarification_questions, clarification_reason = self.clarification_service.clarify_before_context(
            request.question
        )
        if needs_clarification:
            logger.info(
                "数据分析问题前置澄清: question=%s, clarification_questions=%s",
                request.question,
                clarification_questions,
            )
            return DataAnalysisGenerateSqlResponse(
                question=request.question,
                candidateTables=[],
                needsClarification=True,
                clarificationQuestions=clarification_questions,
                reason=clarification_reason,
                warnings=["请先补充分析条件，再生成 SQL。"],
            )
        metrics = self._safe_search_metrics(request.question)
        metric_tables = [table for metric in metrics for table in metric.tables]
        plan_response = self.plan_service.plan(
            DataAnalysisPlanRequest(
                question=request.question,
                tableCatalog=request.tableCatalog,
            )
        )
        logger.info(
            "数据分析 generate_sql: question=%s, matched_metric_codes=%s, plan_tables=%s",
            request.question,
            [metric.code for metric in metrics],
            plan_response.candidateTables,
        )
        candidate_tables = self._merge_candidate_tables(
            metric_tables=metric_tables,
            plan_tables=plan_response.candidateTables,
        )
        if not candidate_tables:
            return DataAnalysisGenerateSqlResponse(
                question=request.question,
                candidateTables=[],
                warnings=["未能选择到候选表，暂时无法生成 SQL。"],
            )

        prompt_metrics = self._select_prompt_metrics(metrics, candidate_tables)
        logger.info(
            "数据分析 generate_sql: candidate_tables=%s, prompt_metric_codes=%s",
            candidate_tables,
            [metric.code for metric in prompt_metrics],
        )

        schema_response = self.schema_service.get_schemas(
            DataAnalysisSchemaRequest(tables=candidate_tables)
        )
        if schema_response.missingTables:
            return DataAnalysisGenerateSqlResponse(
                question=request.question,
                candidateTables=candidate_tables,
                warnings=["部分候选表缺少 DDL，暂时无法生成可靠 SQL。"],
                missingTables=schema_response.missingTables,
            )

        # knowledge_missing, knowledge_missing_items, knowledge_missing_reason = self.knowledge_gap_service.detect(
        #     request.question,
        #     schema_response.schemas,
        # )
        # if knowledge_missing:
        #     logger.info(
        #         "数据分析知识缺口: question=%s, candidate_tables=%s, missing_items=%s",
        #         request.question,
        #         candidate_tables,
        #         knowledge_missing_items,
        #     )
        #     return DataAnalysisGenerateSqlResponse(
        #         question=request.question,
        #         candidateTables=candidate_tables,
        #         knowledgeMissing=True,
        #         knowledgeMissingItems=knowledge_missing_items,
        #         reason=knowledge_missing_reason,
        #         warnings=["候选表结构知识不完整，请先补充数据分析知识库。"],
        #     )

        needs_clarification, clarification_questions, clarification_reason = self.clarification_service.clarify_after_context(
            request.question,
            metric_contexts=[metric.content for metric in prompt_metrics],
            schema_contexts=[schema.content for schema in schema_response.schemas],
        )
        if needs_clarification:
            logger.info(
                "数据分析问题上下文澄清: question=%s, candidate_tables=%s, prompt_metric_codes=%s, clarification_questions=%s",
                request.question,
                candidate_tables,
                [metric.code for metric in prompt_metrics],
                clarification_questions,
            )
            return DataAnalysisGenerateSqlResponse(
                question=request.question,
                candidateTables=candidate_tables,
                needsClarification=True,
                clarificationQuestions=clarification_questions,
                reason=clarification_reason,
                warnings=["请先补充分析条件，再生成 SQL。"],
            )

        sql_plan = self.sql_generation_service.generate(
            question=request.question,
            candidate_tables=candidate_tables,
            schemas=schema_response.schemas,
            max_rows=request.maxRows,
            metrics=prompt_metrics,
        )
        if not sql_plan.sql:
            return DataAnalysisGenerateSqlResponse(
                question=request.question,
                candidateTables=candidate_tables,
                reason=sql_plan.reason,
                assumptions=sql_plan.assumptions,
                warnings=sql_plan.warnings or ["模型未返回 SQL。"],
            )

        safe_sql = self.sql_guard.validate(
            sql_plan.sql,
            allowed_tables=candidate_tables,
            max_rows=request.maxRows,
        )
        return DataAnalysisGenerateSqlResponse(
            question=request.question,
            candidateTables=candidate_tables,
            sql=safe_sql,
            reason=sql_plan.reason,
            assumptions=sql_plan.assumptions,
            warnings=sql_plan.warnings,
            missingTables=[],
        )

    def _merge_candidate_tables(self, *, metric_tables: list[str], plan_tables: list[str]) -> list[str]:
        result: list[str] = []
        for table in metric_tables + plan_tables:
            value = str(table or "").strip()
            if value and value not in result:
                result.append(value)
        return result

    def _safe_search_metrics(self, question: str) -> list[DataAnalysisMetricItem]:
        try:
            response = self.metric_service.search_metrics(
                DataAnalysisMetricSearchRequest(question=question, topK=3)
            )
            logger.info(
                "数据分析指标召回完成: question=%s, result_count=%s, metric_codes=%s",
                question,
                len(response.metrics),
                [metric.code for metric in response.metrics],
            )
            return response.metrics
        except Exception:
            # 指标知识库是增强上下文。即使尚未入库或 Milvus 临时不可用，也不能阻断原有 Text-to-SQL 链路。
            logger.exception("数据分析指标召回失败，将降级走原有 Text-to-SQL 链路: question=%s", question)
            return []

    def _select_prompt_metrics(
        self,
        metrics: list[DataAnalysisMetricItem],
        candidate_tables: list[str],
    ) -> list[DataAnalysisMetricItem]:
        candidate_table_set = {str(table).strip() for table in candidate_tables if str(table).strip()}
        if not candidate_table_set:
            return []

        filtered_metrics: list[DataAnalysisMetricItem] = []
        for metric in metrics:
            metric_table_set = {str(table).strip() for table in metric.tables if str(table).strip()}
            if not metric_table_set or metric_table_set & candidate_table_set:
                filtered_metrics.append(metric)

        # prompt 里放太多口径会互相干扰，先保留前 2 条最高相关候选。
        return filtered_metrics[:2]
