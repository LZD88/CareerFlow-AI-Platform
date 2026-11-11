package cn.yq.common.dataConfig.mapper;




import cn.yq.common.dataConfig.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置 Mapper 接口
 */
@Mapper
public interface SysConfigMapper {

    /**
     * 插入配置
     *
     * @param entity 配置实体
     * @return 影响行数
     */
    int insert(SysConfigEntity entity);

    /**
     * 根据ID删除
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID更新
     *
     * @param entity 配置实体
     * @return 影响行数
     */
    int updateById(SysConfigEntity entity);

    /**
     * 根据ID查询
     *
     * @param id 主键ID
     * @return 配置实体
     */
    SysConfigEntity selectById(@Param("id") Long id);

    /**
     * 根据配置Key查询
     *
     */
    String getValue(@Param("k") String k);

    SysConfigEntity selectByKey(@Param("k") String k);

    List<SysConfigEntity> selectPage(@Param("keyword") String keyword);

    /**
     * 查询全部配置
     *
     * @return 配置列表
     */
    List<SysConfigEntity> selectAll();
}
