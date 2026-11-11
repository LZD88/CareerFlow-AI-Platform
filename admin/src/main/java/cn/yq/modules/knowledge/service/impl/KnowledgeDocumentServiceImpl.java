package cn.yq.modules.knowledge.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.config.PythonAgentProperties;
import cn.yq.modules.knowledge.domain.entity.KnowledgeDocumentEntity;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentChunkQueryReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentCreateReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentQueryReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentUpdateReq;
import cn.yq.modules.knowledge.domain.vo.req.PythonKnowledgeDeleteDocumentChunksReq;
import cn.yq.modules.knowledge.domain.vo.req.PythonKnowledgeDocumentChunksReq;
import cn.yq.modules.knowledge.domain.vo.req.PythonKnowledgeIngestTextReq;
import cn.yq.modules.knowledge.domain.vo.res.KnowledgeDocumentChunkRes;
import cn.yq.modules.knowledge.domain.vo.res.KnowledgeDocumentUploadRes;
import cn.yq.modules.knowledge.domain.vo.res.PythonKnowledgeDeleteDocumentChunksRes;
import cn.yq.modules.knowledge.domain.vo.res.PythonKnowledgeDocumentChunksRes;
import cn.yq.modules.knowledge.domain.vo.res.PythonKnowledgeIngestTextRes;
import cn.yq.modules.knowledge.mapper.KnowledgeDocumentMapper;
import cn.yq.modules.knowledge.service.KnowledgeDocumentService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private static final String STATUS_INDEXING = "INDEXING";
    private static final String STATUS_INDEXED = "INDEXED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DISABLED = "DISABLED";
    // 只保存到 MySQL、暂不写入 Milvus 的状态；代码知识库批量上传时会用到。
    private static final String STATUS_PENDING = "PENDING";
    // 生成脚本产出的 Java 方法级 Markdown，Python 会按二级标题切成方法 chunk。
    private static final String SOURCE_TYPE_GENERATED_JAVA_MD = "generated_java_md";
    private static final String SOURCE_TYPE_DATA_ANALYSIS_SCHEMA_MD = "data_analysis_schema_md";
    private static final String SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON = "data_analysis_metric_json";
    // 代码诊断知识库不绑定课程，用 permissionScope 和学生知识库隔开。
    private static final String PERMISSION_SCOPE_LOG_DIAGNOSIS = "log_diagnosis";
    private static final String PERMISSION_SCOPE_DATA_ANALYSIS_SCHEMA = "data_analysis_schema";
    private static final String PERMISSION_SCOPE_DATA_ANALYSIS_METRIC = "data_analysis_metric";
    private static final String KNOWLEDGE_DOMAIN_LOG_DIAGNOSIS = "LOG_DIAGNOSIS";
    private static final String KNOWLEDGE_DOMAIN_DATA_ANALYSIS_SCHEMA = "DATA_ANALYSIS_SCHEMA";
    private static final String KNOWLEDGE_DOMAIN_DATA_ANALYSIS_METRIC = "DATA_ANALYSIS_METRIC";

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final PythonAgentProperties pythonAgentProperties;

    @Override
    public PageResult<KnowledgeDocumentEntity> page(KnowledgeDocumentQueryReq request) {
        Page<KnowledgeDocumentEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<KnowledgeDocumentEntity> records = knowledgeDocumentMapper.selectPage(
                request.getKeyword(),
                request.getStatus(),
                request.getCourseId(),
                request.getStudyMode(),
                request.getSourceType(),
                request.getPermissionScope()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public KnowledgeDocumentEntity detail(Long id) {
        return requireDocument(id);
    }

    @Override
    public PageResult<KnowledgeDocumentChunkRes> chunks(Long id, KnowledgeDocumentChunkQueryReq request) {
        // 前端只传 MySQL 文档主键；documentCode/version 由后端查库确定，避免越权查询 Milvus。
        KnowledgeDocumentEntity entity = requireDocument(id);
        PythonKnowledgeDocumentChunksRes response = callPythonDocumentChunks(entity, request);
        return new PageResult<>(
                entity.getChunkCount() == null ? 0L : entity.getChunkCount().longValue(),
                request.getPageNum(),
                request.getPageSize(),
                response.getRecords() == null ? List.of() : response.getRecords()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(KnowledgeDocumentCreateReq request) {
        KnowledgeDocumentEntity entity = buildEntity(
                "kd_" + IdUtil.fastSimpleUUID(),
                StrUtil.trim(request.getTitle()),
                request.getContent(),
                StrUtil.blankToDefault(request.getSourceType(), "manual"),
                StrUtil.blankToDefault(request.getSourcePath(), ""),
                request.getCourseId(),
                StrUtil.blankToDefault(request.getStudyMode(), "ALL"),
                StrUtil.blankToDefault(request.getPermissionScope(), "course"),
                STATUS_INDEXING
        );
        indexDocument(entity);
        return entity.getId();
    }

    @Override
    public KnowledgeDocumentUploadRes uploadMarkdownFiles(MultipartFile[] files,
                                                          Boolean indexNow,
                                                          String sourceType,
                                                          String permissionScope) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的 Markdown 文件");
        }
        if (files.length > 50) {
            throw new BusinessException("一次最多上传 50 个 Markdown 文件");
        }

        boolean shouldIndex = !Boolean.FALSE.equals(indexNow);
        String resolvedSourceType = resolveUploadSourceType(sourceType);
        String resolvedPermissionScope = resolveUploadPermissionScope(permissionScope, resolvedSourceType);
        KnowledgeDocumentUploadRes result = new KnowledgeDocumentUploadRes();
        result.setTotal(files.length);
        result.setIndexNow(shouldIndex);

        // 批量上传按文件逐个处理：单个文件失败不影响其他文件落库/入库。
        for (MultipartFile file : files) {
            KnowledgeDocumentUploadRes.Item item = new KnowledgeDocumentUploadRes.Item();
            item.setFileName(file == null ? "" : file.getOriginalFilename());
            try {
                KnowledgeDocumentEntity entity = createMarkdownEntity(file, shouldIndex, resolvedSourceType, resolvedPermissionScope);
                item.setDocumentId(entity.getId());
                item.setDocumentCode(entity.getDocumentCode());
                item.setStatus(entity.getStatus());
                item.setChunkCount(entity.getChunkCount());
                item.setErrorMessage(entity.getErrorMessage());
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                log.error("Markdown 知识文档上传失败: file={}", item.getFileName(), ex);
                item.setStatus(STATUS_FAILED);
                item.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 1000));
                result.setFailed(result.getFailed() + 1);
            }
            result.getItems().add(item);
        }
        return result;
    }

    @Override
    public KnowledgeDocumentUploadRes uploadJsonFiles(MultipartFile[] files,
                                                      Boolean indexNow,
                                                      String sourceType,
                                                      String permissionScope) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的 JSON 文件");
        }
        if (files.length > 50) {
            throw new BusinessException("一次最多上传 50 个 JSON 文件");
        }

        boolean shouldIndex = !Boolean.FALSE.equals(indexNow);
        String resolvedSourceType = resolveUploadJsonSourceType(sourceType);
        String resolvedPermissionScope = resolveUploadPermissionScope(permissionScope, resolvedSourceType);
        KnowledgeDocumentUploadRes result = new KnowledgeDocumentUploadRes();
        result.setTotal(files.length);
        result.setIndexNow(shouldIndex);

        for (MultipartFile file : files) {
            KnowledgeDocumentUploadRes.Item item = new KnowledgeDocumentUploadRes.Item();
            item.setFileName(file == null ? "" : file.getOriginalFilename());
            try {
                KnowledgeDocumentEntity entity = createJsonEntity(file, shouldIndex, resolvedSourceType, resolvedPermissionScope);
                item.setDocumentId(entity.getId());
                item.setDocumentCode(entity.getDocumentCode());
                item.setStatus(entity.getStatus());
                item.setChunkCount(entity.getChunkCount());
                item.setErrorMessage(entity.getErrorMessage());
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                log.error("JSON 知识文档上传失败: file={}", item.getFileName(), ex);
                item.setStatus(STATUS_FAILED);
                item.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 1000));
                result.setFailed(result.getFailed() + 1);
            }
            result.getItems().add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, KnowledgeDocumentUpdateReq request) {
        KnowledgeDocumentEntity entity = requireDocument(id);
        Integer oldVersion = entity.getVersion();
        Integer newVersion = oldVersion == null ? 1 : oldVersion + 1;

        Date now = new Date();
        entity.setTitle(StrUtil.trim(request.getTitle()));
        entity.setContent(request.getContent());
        entity.setSourceType(StrUtil.blankToDefault(request.getSourceType(), "manual"));
        entity.setSourcePath(StrUtil.blankToDefault(request.getSourcePath(), ""));
        entity.setCourseId(request.getCourseId());
        entity.setClassId(null);
        entity.setStudyMode(StrUtil.blankToDefault(request.getStudyMode(), "ALL"));
        entity.setVersion(newVersion);
        entity.setPermissionScope(StrUtil.blankToDefault(request.getPermissionScope(), "course"));
        entity.setStatus(STATUS_INDEXING);
        entity.setChunkCount(0);
        entity.setErrorMessage("");
        entity.setUpdatedAt(now);
        knowledgeDocumentMapper.updateById(entity);

        try {
            PythonKnowledgeIngestTextRes ingestResponse = callPythonIngest(entity);
            // 新版本成功入库后再删旧版本，避免更新失败造成知识库空窗。
            if (oldVersion != null && !oldVersion.equals(newVersion)) {
                callPythonDeleteDocumentChunks(entity.getDocumentCode(), oldVersion);
            }

            entity.setStatus(STATUS_INDEXED);
            entity.setChunkCount(ingestResponse.getChunkCount() == null ? 0 : ingestResponse.getChunkCount());
            entity.setErrorMessage("");
            entity.setIndexedAt(new Date());
            entity.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateById(entity);
        } catch (Exception ex) {
            log.error("知识文档更新入库失败: id={}, documentCode={}, oldVersion={}, newVersion={}",
                    entity.getId(), entity.getDocumentCode(), oldVersion, newVersion, ex);
            entity.setStatus(STATUS_FAILED);
            entity.setChunkCount(0);
            entity.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 1000));
            entity.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reindex(Long id) {
        KnowledgeDocumentEntity entity = requireDocument(id);
        Integer oldVersion = entity.getVersion();
        Integer newVersion = oldVersion == null ? 1 : oldVersion + 1;

        entity.setVersion(newVersion);
        entity.setStatus(STATUS_INDEXING);
        entity.setErrorMessage("");
        entity.setUpdatedAt(new Date());
        knowledgeDocumentMapper.updateById(entity);

        try {
            PythonKnowledgeIngestTextRes ingestResponse = callPythonIngest(entity);
            // 新版本成功入库后再删旧版本，避免重入库失败造成知识库空窗。
            if (oldVersion != null && !oldVersion.equals(newVersion)) {
                callPythonDeleteDocumentChunks(entity.getDocumentCode(), oldVersion);
            }

            entity.setStatus(STATUS_INDEXED);
            entity.setChunkCount(ingestResponse.getChunkCount() == null ? 0 : ingestResponse.getChunkCount());
            entity.setErrorMessage("");
            entity.setIndexedAt(new Date());
            entity.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateById(entity);
        } catch (Exception ex) {
            log.error("知识文档重入库失败: id={}, documentCode={}, oldVersion={}, newVersion={}",
                    entity.getId(), entity.getDocumentCode(), oldVersion, newVersion, ex);
            entity.setStatus(STATUS_FAILED);
            entity.setChunkCount(0);
            entity.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 1000));
            entity.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        KnowledgeDocumentEntity entity = requireDocument(id);
        // 方案一：禁用即删除当前版本 chunk，确保学生端 RAG 不再检索到这份资料。
        callPythonDeleteDocumentChunks(entity.getDocumentCode(), entity.getVersion());
        entity.setStatus(STATUS_DISABLED);
        entity.setChunkCount(0);
        entity.setErrorMessage("");
        entity.setUpdatedAt(new Date());
        knowledgeDocumentMapper.updateById(entity);
    }

    private void indexDocument(KnowledgeDocumentEntity entity) {
        try {
            PythonKnowledgeIngestTextRes response = callPythonIngest(entity);
            entity.setStatus(STATUS_INDEXED);
            entity.setChunkCount(response.getChunkCount() == null ? 0 : response.getChunkCount());
            entity.setErrorMessage("");
            entity.setIndexedAt(new Date());
            entity.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateById(entity);
        } catch (Exception ex) {
            log.error("知识文档入库失败: id={}, documentCode={}", entity.getId(), entity.getDocumentCode(), ex);
            entity.setStatus(STATUS_FAILED);
            entity.setErrorMessage(StrUtil.subPre(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), 1000));
            entity.setUpdatedAt(new Date());
            knowledgeDocumentMapper.updateById(entity);
        }
    }

    private KnowledgeDocumentEntity createMarkdownEntity(MultipartFile file,
                                                         boolean indexNow,
                                                         String sourceType,
                                                         String permissionScope) throws IOException {
        validateMarkdownFile(file);
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "knowledge.md");
        String content = readMarkdownContent(file);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("Markdown 文件内容不能为空: " + originalName);
        }

        String documentCode = resolveDocumentCodePrefix(sourceType) + IdUtil.fastSimpleUUID();
        KnowledgeDocumentEntity entity = buildEntity(
                documentCode,
                resolveMarkdownTitle(originalName, content),
                content,
                sourceType,
                originalName,
                null,
                "ALL",
                permissionScope,
                indexNow ? STATUS_INDEXING : STATUS_PENDING
        );
        // 允许先做文件治理和人工确认，之后再点“重入库”写入 Milvus。
        if (indexNow) {
            indexDocument(entity);
        }
        return entity;
    }

    private KnowledgeDocumentEntity createJsonEntity(MultipartFile file,
                                                     boolean indexNow,
                                                     String sourceType,
                                                     String permissionScope) throws IOException {
        validateJsonFile(file);
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "knowledge.json");
        String content = readJsonContent(file);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("JSON 文件内容不能为空: " + originalName);
        }
        validateJsonContent(content, originalName);

        String documentCode = resolveDocumentCodePrefix(sourceType) + IdUtil.fastSimpleUUID();
        KnowledgeDocumentEntity entity = buildEntity(
                documentCode,
                resolveJsonTitle(originalName, content),
                content,
                sourceType,
                originalName,
                null,
                "ALL",
                permissionScope,
                indexNow ? STATUS_INDEXING : STATUS_PENDING
        );
        if (indexNow) {
            indexDocument(entity);
        }
        return entity;
    }

    private String readMarkdownContent(MultipartFile file) throws IOException {
        // Markdown 由生成脚本产出，约定使用 UTF-8；这里额外处理 BOM 和不同系统换行，避免 Python 按标题切片时漏切。
        String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    private String readJsonContent(MultipartFile file) throws IOException {
        String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }
        return content.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    private KnowledgeDocumentEntity buildEntity(String documentCode,
                                                String title,
                                                String content,
                                                String sourceType,
                                                String sourcePath,
                                                Long courseId,
                                                String studyMode,
                                                String permissionScope,
                                                String status) {
        Date now = new Date();
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setDocumentCode(documentCode);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setSourceType(sourceType);
        entity.setSourcePath(sourcePath);
        entity.setCourseId(courseId);
        entity.setClassId(null);
        entity.setStudyMode(studyMode);
        entity.setVersion(1);
        entity.setPermissionScope(permissionScope);
        entity.setStatus(status);
        entity.setChunkCount(0);
        entity.setErrorMessage("");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        knowledgeDocumentMapper.insert(entity);
        return entity;
    }

    private void validateMarkdownFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Markdown 文件不能为空");
        }
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "");
        String lowerName = originalName.toLowerCase();
        if (!lowerName.endsWith(".md") && !lowerName.endsWith(".markdown")) {
            throw new BusinessException("仅支持上传 .md/.markdown 文件: " + originalName);
        }
    }

    private void validateJsonFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("JSON 文件不能为空");
        }
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "");
        if (!originalName.toLowerCase().endsWith(".json")) {
            throw new BusinessException("仅支持上传 .json 文件: " + originalName);
        }
    }

    private void validateJsonContent(String content, String fileName) {
        try {
            JSON.parse(content);
        } catch (Exception ex) {
            throw new BusinessException("JSON 文件格式不合法: " + fileName);
        }
    }

    private String resolveMarkdownTitle(String fileName, String content) {
        for (String line : content.split("\\R", 20)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return StrUtil.subPre(trimmed.substring(2).trim(), 200);
            }
        }
        String title = fileName.replaceAll("(?i)\\.(md|markdown)$", "");
        return StrUtil.subPre(StrUtil.blankToDefault(title, "Markdown 知识文档"), 200);
    }

    private String resolveJsonTitle(String fileName, String content) {
        try {
            Object parsed = JSON.parse(content);
            if (parsed instanceof JSONObject object) {
                if (StrUtil.isNotBlank(object.getString("title"))) {
                    return StrUtil.subPre(object.getString("title").trim(), 200);
                }
                JSONArray metrics = object.getJSONArray("metrics");
                if (metrics != null && !metrics.isEmpty()) {
                    JSONObject first = metrics.getJSONObject(0);
                    if (first != null && StrUtil.isNotBlank(first.getString("title"))) {
                        return StrUtil.subPre(first.getString("title").trim(), 200);
                    }
                }
            }
            if (parsed instanceof JSONArray array && !array.isEmpty()) {
                JSONObject first = array.getJSONObject(0);
                if (first != null && StrUtil.isNotBlank(first.getString("title"))) {
                    return StrUtil.subPre(first.getString("title").trim(), 200);
                }
            }
        } catch (Exception ignored) {
            // 已在 validateJsonContent 校验过，这里兜底回退到文件名。
        }
        String title = fileName.replaceAll("(?i)\\.json$", "");
        return StrUtil.subPre(StrUtil.blankToDefault(title, "JSON 知识文档"), 200);
    }

    private String resolveUploadSourceType(String sourceType) {
        String value = StrUtil.blankToDefault(sourceType, SOURCE_TYPE_GENERATED_JAVA_MD);
        if (SOURCE_TYPE_DATA_ANALYSIS_SCHEMA_MD.equals(value)) {
            return SOURCE_TYPE_DATA_ANALYSIS_SCHEMA_MD;
        }
        return SOURCE_TYPE_GENERATED_JAVA_MD;
    }

    private String resolveUploadJsonSourceType(String sourceType) {
        String value = StrUtil.blankToDefault(sourceType, SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON);
        if (SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON.equals(value)) {
            return SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON;
        }
        return SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON;
    }

    private String resolveUploadPermissionScope(String permissionScope, String sourceType) {
        if (SOURCE_TYPE_DATA_ANALYSIS_SCHEMA_MD.equals(sourceType)) {
            return PERMISSION_SCOPE_DATA_ANALYSIS_SCHEMA;
        }
        if (SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON.equals(sourceType)) {
            return PERMISSION_SCOPE_DATA_ANALYSIS_METRIC;
        }
        return StrUtil.blankToDefault(permissionScope, PERMISSION_SCOPE_LOG_DIAGNOSIS);
    }

    private String resolveDocumentCodePrefix(String sourceType) {
        if (SOURCE_TYPE_DATA_ANALYSIS_SCHEMA_MD.equals(sourceType)) {
            return "kd_schema_";
        }
        if (SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON.equals(sourceType)) {
            return "kd_metric_";
        }
        return "kd_java_";
    }

    private PythonKnowledgeIngestTextRes callPythonIngest(KnowledgeDocumentEntity entity) {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            throw new BusinessException("Python Agent 未启用");
        }
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 地址未配置");
        }

        PythonKnowledgeIngestTextReq request = new PythonKnowledgeIngestTextReq();
        request.setDocumentId(entity.getDocumentCode());
        request.setTitle(entity.getTitle());
        request.setContent(entity.getContent());
        request.setCourseId(entity.getCourseId());
        request.setClassId(null);
        request.setStudyMode(StrUtil.blankToDefault(entity.getStudyMode(), "ALL"));
        request.setVersion(entity.getVersion());
        request.setSourceType(entity.getSourceType());
        request.setSourcePath(entity.getSourcePath());
        request.setPermissionScope(entity.getPermissionScope());
        // 告诉 Python 选择哪个 Milvus collection，并写入对应知识域动态字段。
        request.setKnowledgeDomain(resolveKnowledgeDomain(entity));

        try (HttpResponse response = HttpRequest.post(buildPythonIngestUrl())
                .timeout(pythonAgentProperties.getTimeoutMs())
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python 入库接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), PythonKnowledgeIngestTextRes.class);
        }
    }

    private PythonKnowledgeDocumentChunksRes callPythonDocumentChunks(KnowledgeDocumentEntity entity,
                                                                      KnowledgeDocumentChunkQueryReq queryReq) {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            throw new BusinessException("Python Agent 未启用");
        }
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 地址未配置");
        }

        PythonKnowledgeDocumentChunksReq request = new PythonKnowledgeDocumentChunksReq();
        request.setDocumentId(entity.getDocumentCode());
        // 只查看当前业务文档的最新版本；更新/重入库成功后会清理旧版本 chunk。
        request.setVersion(entity.getVersion());
        request.setKnowledgeDomain(resolveKnowledgeDomain(entity));
        request.setPageNum(queryReq.getPageNum() == null ? 1 : queryReq.getPageNum());
        request.setPageSize(queryReq.getPageSize() == null ? 20 : queryReq.getPageSize());
        request.setIncludeVector(Boolean.TRUE.equals(queryReq.getIncludeVector()));

        try (HttpResponse response = HttpRequest.post(buildPythonDocumentChunksUrl())
                .timeout(pythonAgentProperties.getTimeoutMs())
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python chunk 查询接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), PythonKnowledgeDocumentChunksRes.class);
        }
    }

    private PythonKnowledgeDeleteDocumentChunksRes callPythonDeleteDocumentChunks(String documentCode, Integer version) {
        if (!Boolean.TRUE.equals(pythonAgentProperties.getEnabled())) {
            throw new BusinessException("Python Agent 未启用");
        }
        if (!StringUtils.hasText(pythonAgentProperties.getBaseUrl())) {
            throw new BusinessException("Python Agent 地址未配置");
        }

        PythonKnowledgeDeleteDocumentChunksReq request = new PythonKnowledgeDeleteDocumentChunksReq();
        request.setDocumentId(documentCode);
        request.setVersion(version);
        request.setKnowledgeDomain(resolveKnowledgeDomainByDocumentCode(documentCode));

        try (HttpResponse response = HttpRequest.post(buildPythonDeleteDocumentChunksUrl())
                .timeout(pythonAgentProperties.getTimeoutMs())
                .contentType(ContentType.JSON.toString())
                .body(JSON.toJSONString(request))
                .execute()) {
            if (!response.isOk()) {
                throw new BusinessException("Python chunk 删除接口返回异常: " + response.getStatus() + ", " + response.body());
            }
            return JSON.parseObject(response.body(), PythonKnowledgeDeleteDocumentChunksRes.class);
        }
    }

    private String buildPythonIngestUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getKnowledgeIngestTextPath())
                ? pythonAgentProperties.getKnowledgeIngestTextPath().trim()
                : "/api/knowledge-base/ingest-text";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String buildPythonDocumentChunksUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getKnowledgeDocumentChunksPath())
                ? pythonAgentProperties.getKnowledgeDocumentChunksPath().trim()
                : "/api/knowledge-base/document-chunks";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String buildPythonDeleteDocumentChunksUrl() {
        String baseUrl = pythonAgentProperties.getBaseUrl().trim();
        String path = StringUtils.hasText(pythonAgentProperties.getKnowledgeDeleteDocumentChunksPath())
                ? pythonAgentProperties.getKnowledgeDeleteDocumentChunksPath().trim()
                : "/api/knowledge-base/delete-document-chunks";
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    private String resolveKnowledgeDomain(KnowledgeDocumentEntity entity) {
        if (PERMISSION_SCOPE_DATA_ANALYSIS_SCHEMA.equals(entity.getPermissionScope())
                || SOURCE_TYPE_DATA_ANALYSIS_SCHEMA_MD.equals(entity.getSourceType())) {
            return KNOWLEDGE_DOMAIN_DATA_ANALYSIS_SCHEMA;
        }
        if (PERMISSION_SCOPE_DATA_ANALYSIS_METRIC.equals(entity.getPermissionScope())
                || SOURCE_TYPE_DATA_ANALYSIS_METRIC_JSON.equals(entity.getSourceType())) {
            return KNOWLEDGE_DOMAIN_DATA_ANALYSIS_METRIC;
        }
        if (PERMISSION_SCOPE_LOG_DIAGNOSIS.equals(entity.getPermissionScope())
                || SOURCE_TYPE_GENERATED_JAVA_MD.equals(entity.getSourceType())) {
            return KNOWLEDGE_DOMAIN_LOG_DIAGNOSIS;
        }
        return "STUDENT_QA";
    }

    private String resolveKnowledgeDomainByDocumentCode(String documentCode) {
        if (!StringUtils.hasText(documentCode)) {
            return "STUDENT_QA";
        }
        if (documentCode.startsWith("kd_schema_")) {
            return KNOWLEDGE_DOMAIN_DATA_ANALYSIS_SCHEMA;
        }
        if (documentCode.startsWith("kd_metric_")) {
            return KNOWLEDGE_DOMAIN_DATA_ANALYSIS_METRIC;
        }
        if (documentCode.startsWith("kd_java_")) {
            return KNOWLEDGE_DOMAIN_LOG_DIAGNOSIS;
        }
        return "STUDENT_QA";
    }

    private KnowledgeDocumentEntity requireDocument(Long id) {
        KnowledgeDocumentEntity entity = knowledgeDocumentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("知识文档不存在");
        }
        return entity;
    }
}
