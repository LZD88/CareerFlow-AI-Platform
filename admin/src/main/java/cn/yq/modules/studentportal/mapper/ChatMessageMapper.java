package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.aichat.ChatMessageEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatMessageMapper {

    int insert(ChatMessageEntity entity);

    List<ChatMessageEntity> selectBySessionIdAndStudentId(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId);

    /**
     * 查询尚未被压缩进 session.summary 的消息。
     */
    List<ChatMessageEntity> selectUncompressedBySessionIdAndStudentId(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId);

    /**
     * 将指定消息及之前的历史消息标记为已压缩。
     */
    int markCompressedUntil(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId, @Param("messageId") Long messageId);
}
