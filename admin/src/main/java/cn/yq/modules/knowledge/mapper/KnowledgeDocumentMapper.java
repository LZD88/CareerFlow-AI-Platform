package cn.yq.modules.knowledge.mapper;

import cn.yq.modules.knowledge.domain.entity.KnowledgeDocumentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeDocumentMapper {

    int insert(KnowledgeDocumentEntity entity);

    int updateById(KnowledgeDocumentEntity entity);

    KnowledgeDocumentEntity selectById(@Param("id") Long id);

    List<KnowledgeDocumentEntity> selectPage(@Param("keyword") String keyword,
                                             @Param("status") String status,
                                             @Param("courseId") Long courseId,
                                             @Param("studyMode") String studyMode,
                                             @Param("sourceType") String sourceType,
                                             @Param("permissionScope") String permissionScope);
}
