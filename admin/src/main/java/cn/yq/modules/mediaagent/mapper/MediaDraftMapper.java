package cn.yq.modules.mediaagent.mapper;

import cn.yq.modules.mediaagent.domain.entity.MediaDraftEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MediaDraftMapper {

    int insert(MediaDraftEntity entity);

    List<MediaDraftEntity> selectByProjectId(@Param("projectId") Long projectId);
}
