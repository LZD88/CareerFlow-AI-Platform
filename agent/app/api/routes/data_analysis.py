from fastapi import APIRouter, HTTPException

from app.models.data_analysis import (
    DataAnalysisGenerateSqlRequest,
    DataAnalysisGenerateSqlResponse,
    DataAnalysisInterpretResultRequest,
    DataAnalysisInterpretResultResponse,
    DataAnalysisMetricSearchRequest,
    DataAnalysisMetricSearchResponse,
    DataAnalysisPlanRequest,
    DataAnalysisPlanResponse,
    DataAnalysisPrepareResponse,
    DataAnalysisSchemaRequest,
    DataAnalysisSchemaResponse,
)
from app.services.data_analysis.agent_service import DataAnalysisAgentService
from app.services.data_analysis.metric_service import DataAnalysisMetricService
from app.services.data_analysis.plan_service import DataAnalysisPlanService
from app.services.data_analysis.result_interpreter import DataAnalysisResultInterpreter
from app.services.data_analysis.schema_service import DataAnalysisSchemaService

router = APIRouter()
plan_service = DataAnalysisPlanService()
schema_service = DataAnalysisSchemaService()
agent_service = DataAnalysisAgentService()
result_interpreter = DataAnalysisResultInterpreter()
metric_service = DataAnalysisMetricService()


@router.post("/data-analysis/plan", response_model=DataAnalysisPlanResponse)
def plan_data_analysis(request: DataAnalysisPlanRequest) -> DataAnalysisPlanResponse:
    try:
        return plan_service.plan(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/data-analysis/schemas", response_model=DataAnalysisSchemaResponse)
def get_data_analysis_schemas(request: DataAnalysisSchemaRequest) -> DataAnalysisSchemaResponse:
    try:
        return schema_service.get_schemas(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/data-analysis/metrics/search", response_model=DataAnalysisMetricSearchResponse)
def search_data_analysis_metrics(request: DataAnalysisMetricSearchRequest) -> DataAnalysisMetricSearchResponse:
    try:
        return metric_service.search_metrics(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/data-analysis/prepare", response_model=DataAnalysisPrepareResponse)
def prepare_data_analysis(request: DataAnalysisPlanRequest) -> DataAnalysisPrepareResponse:
    try:
        plan_response = plan_service.plan(request)
        schema_response = (
            schema_service.get_schemas(DataAnalysisSchemaRequest(tables=plan_response.candidateTables))
            if plan_response.candidateTables
            else DataAnalysisSchemaResponse(schemas=[], missingTables=[])
        )
        return DataAnalysisPrepareResponse(
            candidateTables=plan_response.candidateTables,
            reason=plan_response.reason,
            schemas=schema_response.schemas,
            missingTables=schema_response.missingTables,
        )
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/data-analysis/generate-sql", response_model=DataAnalysisGenerateSqlResponse)
def generate_data_analysis_sql(request: DataAnalysisGenerateSqlRequest) -> DataAnalysisGenerateSqlResponse:
    try:
        return agent_service.generate_sql(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@router.post("/data-analysis/interpret-result", response_model=DataAnalysisInterpretResultResponse)
def interpret_data_analysis_result(request: DataAnalysisInterpretResultRequest) -> DataAnalysisInterpretResultResponse:
    try:
        return result_interpreter.interpret(request)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
