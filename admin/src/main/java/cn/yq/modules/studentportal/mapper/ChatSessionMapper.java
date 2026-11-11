package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.aichat.ChatSessionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ChatSessionMapper {

    int insert(ChatSessionEntity entity);

    ChatSessionEntity selectById(@Param("id") Long id);

    ChatSessionEntity selectByIdAndStudentId(@Param("id") Long id, @Param("studentId") Long studentId);

    List<ChatSessionEntity> selectByStudentId(@Param("studentId") Long studentId);

    int updateTitleIfBlank(@Param("id") Long id, @Param("studentId") Long studentId, @Param("title") String title, @Param("updatedAt") Date updatedAt);

    int updateStats(@Param("id") Long id, @Param("studentId") Long studentId, @Param("lastMessageAt") Date lastMessageAt, @Param("updatedAt") Date updatedAt);

    /**
     * 更新会话长期摘要和摘要覆盖到的最后一条消息。
     */
    int updateSummary(@Param("id") Long id,
                      @Param("studentId") Long studentId,
                      @Param("summary") String summary,
                      @Param("summaryTokenCount") Integer summaryTokenCount,
                      @Param("compressedUntilMessageId") Long compressedUntilMessageId,
                      @Param("updatedAt") Date updatedAt);

    int markDeleted(@Param("id") Long id, @Param("studentId") Long studentId, @Param("updatedAt") Date updatedAt);
}
