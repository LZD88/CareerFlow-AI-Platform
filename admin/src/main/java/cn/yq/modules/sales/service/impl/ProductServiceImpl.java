package cn.yq.modules.sales.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.sales.domain.entity.ProductEntity;
import cn.yq.modules.sales.domain.vo.req.ProductCreateReq;
import cn.yq.modules.sales.domain.vo.req.ProductQueryReq;
import cn.yq.modules.sales.domain.vo.req.ProductUpdateReq;
import cn.yq.modules.sales.mapper.ProductMapper;
import cn.yq.modules.sales.service.ProductService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String STUDY_MODE_ONLINE = "ONLINE";
    private static final String STUDY_MODE_OFFLINE = "OFFLINE";

    private final ProductMapper productMapper;

    @Override
    public PageResult<ProductEntity> page(ProductQueryReq request) {
        Page<ProductEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<ProductEntity> records = productMapper.selectPage(request.getKeyword());
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public ProductEntity detail(Long id) {
        return requireProduct(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductCreateReq request) {
        validateProductCodeUnique(request.getProductCode(), null);
        ProductEntity entity = new ProductEntity();
        entity.setProductCode(request.getProductCode().trim());
        entity.setProductName(request.getProductName().trim());
        entity.setStudyMode(normalizeStudyMode(request.getStudyMode()));
        entity.setProductPrice(request.getProductPrice());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        productMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductUpdateReq request) {
        ProductEntity entity = requireProduct(id);
        validateProductCodeUnique(request.getProductCode(), id);
        entity.setProductCode(request.getProductCode().trim());
        entity.setProductName(request.getProductName().trim());
        entity.setStudyMode(normalizeStudyMode(request.getStudyMode()));
        entity.setProductPrice(request.getProductPrice());
        entity.setUpdatedAt(new Date());
        productMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireProduct(id);
        productMapper.deleteById(id);
    }

    private ProductEntity requireProduct(Long id) {
        ProductEntity entity = productMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("产品不存在");
        }
        return entity;
    }

    private void validateProductCodeUnique(String productCode, Long currentId) {
        ProductEntity existing = productMapper.selectByCode(productCode.trim());
        if (existing != null && !Objects.equals(existing.getId(), currentId)) {
            throw new BusinessException("产品编号已存在");
        }
    }

    private String normalizeStudyMode(String studyMode) {
        String normalized = studyMode == null ? "" : studyMode.trim().toUpperCase();
        if (!STUDY_MODE_ONLINE.equals(normalized) && !STUDY_MODE_OFFLINE.equals(normalized)) {
            throw new BusinessException("学习方式仅支持 ONLINE 或 OFFLINE");
        }
        return normalized;
    }
}
