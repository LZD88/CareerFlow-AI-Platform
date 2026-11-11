package cn.yq.modules.knowledge.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.knowledge.domain.entity.KnowledgeDocumentEntity;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentChunkQueryReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentCreateReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentQueryReq;
import cn.yq.modules.knowledge.domain.vo.req.KnowledgeDocumentUpdateReq;
import cn.yq.modules.knowledge.domain.vo.res.KnowledgeDocumentChunkRes;
import cn.yq.modules.knowledge.domain.vo.res.KnowledgeDocumentUploadRes;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeDocumentService {

    PageResult<KnowledgeDocumentEntity> page(KnowledgeDocumentQueryReq request);

    KnowledgeDocumentEntity detail(Long id);

    PageResult<KnowledgeDocumentChunkRes> chunks(Long id, KnowledgeDocumentChunkQueryReq request);

    Long create(KnowledgeDocumentCreateReq request);

    KnowledgeDocumentUploadRes uploadMarkdownFiles(MultipartFile[] files, Boolean indexNow, String sourceType, String permissionScope);

    KnowledgeDocumentUploadRes uploadJsonFiles(MultipartFile[] files, Boolean indexNow, String sourceType, String permissionScope);

    void update(Long id, KnowledgeDocumentUpdateReq request);

    void reindex(Long id);

    void disable(Long id);
}
