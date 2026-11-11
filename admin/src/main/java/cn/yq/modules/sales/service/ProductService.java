package cn.yq.modules.sales.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.sales.domain.entity.ProductEntity;
import cn.yq.modules.sales.domain.vo.req.ProductCreateReq;
import cn.yq.modules.sales.domain.vo.req.ProductQueryReq;
import cn.yq.modules.sales.domain.vo.req.ProductUpdateReq;

public interface ProductService {

    PageResult<ProductEntity> page(ProductQueryReq request);

    ProductEntity detail(Long id);

    Long create(ProductCreateReq request);

    void update(Long id, ProductUpdateReq request);

    void delete(Long id);
}
