package cn.yq.modules.mediaagent.mapper;

import cn.yq.modules.mediaagent.domain.entity.MediaProjectEntity;
import org.apache.ibatis.annotations.Param;

public interface MediaProjectMapper {

    int insert(MediaProjectEntity entity);

    MediaProjectEntity selectById(@Param("id") Long id);
}
