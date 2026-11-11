package cn.yq.modules.dataanalysis.service.impl;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.PythonAgentProperties;
import cn.yq.modules.dataanalysis.domain.vo.req.DataAnalysisAnalyzeReq;
import cn.yq.modules.dataanalysis.domain.vo.req.DataAnalysisPlanReq;
import cn.yq.modules.dataanalysis.domain.vo.req.PythonDataAnalysisGenerateSqlReq;
import cn.yq.modules.dataanalysis.domain.vo.req.PythonDataAnalysisInterpretResultReq;
import cn.yq.modules.dataanalysis.domain.vo.res.DataAnalysisAnalyzeRes;
import cn.yq.modules.dataanalysis.domain.vo.req.PythonDataAnalysisPlanReq;
import cn.yq.modules.dataanalysis.domain.vo.res.DataAnalysisPlanRes;
import cn.yq.modules.dataanalysis.domain.vo.res.PythonDataAnalysisGenerateSqlRes;
import cn.yq.modules.dataanalysis.domain.vo.res.PythonDataAnalysisInterpretResultRes;
import cn.yq.modules.dataanalysis.domain.vo.res.PythonDataAnalysisPlanRes;
import cn.yq.modules.dataanalysis.service.DataAnalysisService;
import cn.yq.modules.dataanalysis.support.DataAnalysisSqlGuard;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataAnalysisServiceImpl implements DataAnalysisService {

    /**
     * 当前数据分析链路采用“Python 生成 SQL，Java 执行 SQL”的边界：
     * 1. Python 更适合做 LLM 编排、选表、读 Milvus 表结构、生成 SQL；
     * 2. Java 更适合承载后台用户鉴权、SQL 最终校验、数据库连接和审计；
     * 3. Python 不接触业务库账号，避免把高风险查询能力暴露给 Agent 服务。
     */
    private final PythonAgentProperties pythonAgentProperties;
    private final JdbcTemplate jdbcTemplate;
    private final DataAnalysisSqlGuard sqlGuard;

    @Override
    public DataAnalysisPlanRes plan(DataAnalysisPlanReq request) {
        PythonDataAnalysisPlanReq pythonRequest = new PythonDataAnalysisPlanReq();
        pythonRequest.setQuestion(request.getQuestion());

        PythonDataAnalysisPlanRes pythonResponse = callPythonPlan(pythonRequest);
        DataAnalysisPlanRes response = new DataAnalysisPlanRes();
        response.setCandidateTables(pythonResponse.getCandidateTables());
        response.setReason(pythonResponse.getReason());
        response.setNeedSchema(Boolean.TRUE.equals(pythonResponse.getNeedSchema()));
        response.setSchemas(pythonResponse.getSchemas());
        response.setMissingTables(pythonResponse.getMissingTables());
        return response;
    }

    @Override
    public DataAnalysisAnalyzeRes analyze(DataAnalysisAnalyzeReq request) {
        // maxRows 既会传给 Python 约束 SQL，也会在 Java SQL Guard 中再次兜底。
        // 即使模型没有按要求补 LIMIT，Java 仍会追加或收紧 LIMIT。
        int maxRows = resolveMaxRows(request.getMaxRows());

        // 第一步：让 Python Agent 基于用户问题、候选表和 Milvus DDL 生成 SQL。
        // Java 不把数据库结果提前给 Python，Python 此时只负责“如何查”。
        PythonDataAnalysisGenerateSqlReq generateSqlReq = new PythonDataAnalysisGenerateSqlReq();
        generateSqlReq.setQuestion(request.getQuestion());
        generateSqlReq.setMaxRows(maxRows);

        PythonDataAnalysisGenerateSqlRes generateSqlRes = callPythonGenerateSql(generateSqlReq);
        if (!StringUtils.hasText(generateSqlRes.getSql())) {
            // Python 可能因为缺少 DDL、问题不清晰或模型主动拒绝而不返回 SQL。
            // 这种情况直接把候选表、假设和 warning 返回给前端，不执行任何数据库查询。
            DataAnalysisAnalyzeRes emptyResponse = new DataAnalysisAnalyzeRes();
            emptyResponse.setQuestion(request.getQuestion());
            emptyResponse.setCandidateTables(safeList(generateSqlRes.getCandidateTables()));
            emptyResponse.setReason(generateSqlRes.getReason());
            emptyResponse.setNeedsClarification(Boolean.TRUE.equals(generateSqlRes.getNeedsClarification()));
            emptyResponse.setClarificationQuestions(safeList(generateSqlRes.getClarificationQuestions()));
            emptyResponse.setKnowledgeMissing(Boolean.TRUE.equals(generateSqlRes.getKnowledgeMissing()));
            emptyResponse.setKnowledgeMissingItems(safeList(generateSqlRes.getKnowledgeMissingItems()));
            emptyResponse.setAssumptions(safeList(generateSqlRes.getAssumptions()));
            emptyResponse.setWarnings(safeList(generateSqlRes.getWarnings()));
            emptyResponse.setMissingTables(safeList(generateSqlRes.getMissingTables()));
            return emptyResponse;
        }

        // 第二步：Java 做最终 SQL 安全校验。
        // 注意 Python 侧也有 SQL Guard，但只能作为第一层兜底；真正执行前必须由 Java 再校验。
        String safeSql = sqlGuard.validate(generateSqlRes.getSql(), generateSqlRes.getCandidateTables(), maxRows);

        // 第三步：执行只读 SQL。当前先复用主数据源跑通链路，生产建议切换到单独只读数据源。
        List<Map<String, Object>> rows = executeQuery(safeSql);
        List<String> columns = resolveColumns(rows);
        // 这里用 rows.size() >= maxRows 表示“可能被 LIMIT 截断”。
        // 严格 total 需要额外 count 查询，第一版先避免为每个分析请求多打一条 SQL。
        boolean truncated = rows.size() >= maxRows;

        // 第四步：把查询结果样本交给 Python 做自然语言解读。
        // Python 仍然不查库，只基于 Java 已经返回的 rows/columns/sql 做总结。
        PythonDataAnalysisInterpretResultReq interpretReq = new PythonDataAnalysisInterpretResultReq();
        interpretReq.setQuestion(request.getQuestion());
        interpretReq.setSql(safeSql);
        interpretReq.setColumns(columns);
        interpretReq.setRows(rows);
        interpretReq.setRowCount(rows.size());
        interpretReq.setTruncated(truncated);
        interpretReq.setAssumptions(safeList(generateSqlRes.getAssumptions()));
        interpretReq.setWarnings(safeList(generateSqlRes.getWarnings()));
        PythonDataAnalysisInterpretResultRes interpretRes = null;
        List<String> warnings = new ArrayList<>(safeList(generateSqlRes.getWarnings()));
        try {
            interpretRes = callPythonInterpretResult(interpretReq);
        } catch (Exception ex) {
            // 结果解释是增强能力，不应该影响 SQL 查询结果返回。
            // 即使模型服务临时失败，前端仍然可以展示 SQL 和表格数据。
            log.error("Python 数据分析结果解释失败，保留 SQL 查询结果返回: sql={}", safeSql, ex);
            warnings.add("结果解释生成失败，仅返回 SQL 查询结果。");
        }

        // 最后聚合 Python SQL 计划、Java 查询结果和 Python 结果解释，返回给前端。
        DataAnalysisAnalyzeRes response = new DataAnalysisAnalyzeRes();
        response.setQuestion(request.getQuestion());
        response.setCandidateTables(safeList(generateSqlRes.getCandidateTables()));
        response.setSql(safeSql);
        response.setReason(generateSqlRes.getReason());
        response.setNeedsClarification(Boolean.FALSE);
        response.setClarificationQuestions(new ArrayList<>());
        response.setKnowledgeMissing(Boolean.FALSE);
        response.setKnowledgeMissingItems(new ArrayList<>());
        response.setColumns(columns);
        response.setRows(rows);
        response.setRowCount(rows.size());
        response.setTruncated(truncated);
        response.setAnalysis(interpretRes == null ? "" : interpretRes.getAnalysis());
        response.setInsights(interpretRes == null ? new ArrayList<>() : safeList(interpretRes.getInsights()));
        response.setFollowUpQuestions(interpretRes == null ? new ArrayList<>() : safeList(interpretRes.getFollowUpQuestions()));
        response.setChartSuggestion(interpretRes == null ? null : interpretRes.getChartSuggestion());
        response.setAssumptions(safeList(generateSqlRes.getAssumptions()));
        response.setWarnings(warnings);
        response.setMissingTables(safeList(generateSqlRes.getMissingTables()));
        return response;
    }

    private PythonDataAnalysisPlanRes callPythonPlan(PythonDataAnalysisPlanReq request) {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            throw new BusinessException("Python Agent 未启用");
        }
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 地址未配置");
        }

        try (HttpResponse response = HttpRequest.post(buildPythonPlanUrl())
                .timeout(pythonAgentProperties.getTimeoutMs())
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python 数据分析选表接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), PythonDataAnalysisPlanRes.class);
        }
    }

    private PythonDataAnalysisGenerateSqlRes callPythonGenerateSql(PythonDataAnalysisGenerateSqlReq request) {
        validatePythonConfig();
        try (HttpResponse response = HttpRequest.post(buildPythonGenerateSqlUrl())
                .timeout(resolvePythonTimeout(pythonAgentProperties.getDataAnalysisGenerateSqlTimeoutMs()))
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python 数据分析 SQL 生成接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), PythonDataAnalysisGenerateSqlRes.class);
        } catch (cn.hutool.http.HttpException ex) {
            log.error("Python 数据分析 SQL 生成接口调用超时或网络异常", ex);
            throw new BusinessException("Python 数据分析 SQL 生成超时，请稍后重试或缩小分析问题范围");
        }
    }

    private PythonDataAnalysisInterpretResultRes callPythonInterpretResult(PythonDataAnalysisInterpretResultReq request) {
        validatePythonConfig();
        try (HttpResponse response = HttpRequest.post(buildPythonInterpretResultUrl())
                .timeout(resolvePythonTimeout(pythonAgentProperties.getDataAnalysisInterpretResultTimeoutMs()))
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python 数据分析结果解释接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), PythonDataAnalysisInterpretResultRes.class);
        } catch (cn.hutool.http.HttpException ex) {
            log.error("Python 数据分析结果解释接口调用超时或网络异常", ex);
            throw new BusinessException("Python 数据分析结果解释超时");
        }
    }

    private void validatePythonConfig() {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            throw new BusinessException("Python Agent 未启用");
        }
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 地址未配置");
        }
    }

    private List<Map<String, Object>> executeQuery(String sql) {
        try {
            // JdbcTemplate 会把每行转换成 LinkedCaseInsensitiveMap，序列化给前端时就是普通 JSON 对象。
            // SQL 已经通过 DataAnalysisSqlGuard，只允许 SELECT/WITH，并且限制候选表和 LIMIT。
            return jdbcTemplate.queryForList(sql);
        } catch (Exception ex) {
            log.error("数据分析 SQL 执行失败: sql={}", sql, ex);
            throw new BusinessException("数据分析 SQL 执行失败: " + ex.getMessage());
        }
    }

    private List<String> resolveColumns(List<Map<String, Object>> rows) {
        // 从返回行里推导列名，保持数据库返回顺序。
        // 如果结果为空，columns 会为空；后续如前端需要空结果也展示列头，可以让 Python SQL 返回预期列名再补充。
        LinkedHashSet<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }
        return new ArrayList<>(columns);
    }

    private int resolveMaxRows(Integer maxRows) {
        if (maxRows == null) {
            return 100;
        }
        // 第一版全局限制 1-500 行，避免模型生成明细查询时把大表直接拖回前端和 Python。
        return Math.min(Math.max(maxRows, 1), 500);
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? new ArrayList<>() : value;
    }

    private int resolvePythonTimeout(Integer timeoutMs) {
        if (timeoutMs != null && timeoutMs > 0) {
            return timeoutMs;
        }
        if (pythonAgentProperties.getTimeoutMs() != null && pythonAgentProperties.getTimeoutMs() > 0) {
            return pythonAgentProperties.getTimeoutMs();
        }
        return 60000;
    }

    private String buildPythonPlanUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getDataAnalysisPlanPath())
                ? pythonAgentProperties.getDataAnalysisPlanPath().trim()
                : "/api/data-analysis/plan";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String buildPythonGenerateSqlUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getDataAnalysisGenerateSqlPath())
                ? pythonAgentProperties.getDataAnalysisGenerateSqlPath().trim()
                : "/api/data-analysis/generate-sql";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String buildPythonInterpretResultUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getDataAnalysisInterpretResultPath())
                ? pythonAgentProperties.getDataAnalysisInterpretResultPath().trim()
                : "/api/data-analysis/interpret-result";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

}
