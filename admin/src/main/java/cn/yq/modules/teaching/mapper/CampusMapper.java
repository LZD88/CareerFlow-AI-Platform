package cn.yq.modules.teaching.mapper;

import cn.yq.modules.teaching.domain.entity.CampusEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CampusMapper {

    int insert(CampusEntity campus);

    int updateById(CampusEntity campus);

    CampusEntity selectById(@Param("id") Long id);

    List<CampusEntity> selectPage(@Param("keyword") String keyword,
                                  @Param("status") Integer status,
                                  @Param("principalUserId") Long principalUserId);

    int deleteById(@Param("id") Long id);
}
