package cn.yq.modules.sales.mapper;

import cn.yq.modules.sales.domain.entity.ProductEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {

    int insert(ProductEntity entity);

    int updateById(ProductEntity entity);

    ProductEntity selectById(@Param("id") Long id);

    ProductEntity selectByCode(@Param("productCode") String productCode);

    List<ProductEntity> selectPage(@Param("keyword") String keyword);

    int deleteById(@Param("id") Long id);
}
