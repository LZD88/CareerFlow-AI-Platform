package cn.yq.modules.knowledge.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.knowledge.domain.entity.KnowledgeDocumentEntity;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentChunkQueryReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentCreateReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentQueryReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentUpdateReq;
import cn.yq.modules.knowledge.domain.vo.res.KnowledgeDocumentChunkRes;
import cn.yq.modules.knowledge.domain.vo.res.KnowledgeDocumentUploadRes;
import cn.yq.modules.knowledge.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @GetMapping
    public ApiResponse<PageResult<KnowledgeDocumentEntity>> page(KnowledgeDocumentQueryReq request) {
        return ApiResponse.success(knowledgeDocumentService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocumentEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(knowledgeDocumentService.detail(id));
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<PageResult<KnowledgeDocumentChunkRes>> chunks(@PathVariable Long id,
                                                                     KnowledgeDocumentChunkQueryReq request) {
        return ApiResponse.success(knowledgeDocumentService.chunks(id, request));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid KnowledgeDocumentCreateReq request) {
        return ApiResponse.success("入库成功", knowledgeDocumentService.create(request));
    }

    @PostMapping("/upload-md")
    public ApiResponse<KnowledgeDocumentUploadRes> uploadMarkdownFiles(@RequestPart("files") MultipartFile[] files,
                                                                       @RequestParam(defaultValue = "true") Boolean indexNow,
                                                                       @RequestParam(required = false) String sourceType,
                                                                       @RequestParam(required = false) String permissionScope) {
        return ApiResponse.success("上传完成", knowledgeDocumentService.uploadMarkdownFiles(files, indexNow, sourceType, permissionScope));
    }

    @PostMapping("/upload-json")
    public ApiResponse<KnowledgeDocumentUploadRes> uploadJsonFiles(@RequestPart("files") MultipartFile[] files,
                                                                   @RequestParam(defaultValue = "true") Boolean indexNow,
                                                                   @RequestParam(required = false) String sourceType,
                                                                   @RequestParam(required = false) String permissionScope) {
        return ApiResponse.success("上传完成", knowledgeDocumentService.uploadJsonFiles(files, indexNow, sourceType, permissionScope));
    }

    @PostMapping("/{id}/update")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody @Valid KnowledgeDocumentUpdateReq request) {
        knowledgeDocumentService.update(id, request);
        return ApiResponse.success("更新已提交入库，请查看状态", null);
    }

    @PostMapping("/{id}/reindex")
    public ApiResponse<Void> reindex(@PathVariable Long id) {
        knowledgeDocumentService.reindex(id);
        return ApiResponse.success("重新入库成功", null);
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        knowledgeDocumentService.disable(id);
        return ApiResponse.success("禁用成功", null);
    }
}
