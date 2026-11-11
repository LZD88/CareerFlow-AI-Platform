package cn.yq.modules.studentportal.mapper;

import cn.yq.modules.studentportal.domain.entity.mockinterview.MockInterviewSessionEntity;
import org.apache.ibatis.annotations.Param;

public interface MockInterviewSessionMapper {

    int insert(MockInterviewSessionEntity entity);

    MockInterviewSessionEntity selectByIdAndStudentId(@Param("id") Long id, @Param("studentId") Long studentId);

    int updateRealtimeToken(@Param("id") Long id,
                            @Param("realtimeToken") String realtimeToken,
                            @Param("realtimeTokenExpiresAt") java.util.Date realtimeTokenExpiresAt);
}
