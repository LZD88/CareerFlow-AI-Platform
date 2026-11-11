package cn.yq.modules.sales.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.sales.domain.entity.ProductEntity;
import cn.yq.modules.sales.domain.vo.req.ProductCreateReq;
import cn.yq.modules.sales.domain.vo.req.ProductQueryReq;
import cn.yq.modules.sales.domain.vo.req.ProductUpdateReq;
import cn.yq.modules.sales.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 产品分页查询。
     */
    @GetMapping
    public ApiResponse<PageResult<ProductEntity>> page(ProductQueryReq request) {
        return ApiResponse.success(productService.page(request));
    }

    /**
     * 查询产品详情，供编辑弹窗回填使用。
     */
    @GetMapping("/{id}")
    public ApiResponse<ProductEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(productService.detail(id));
    }

    /**
     * 创建产品。
     */
    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid ProductCreateReq request) {
        return ApiResponse.success("创建成功", productService.create(request));
    }

    /**
     * 更新产品基础信息。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid ProductUpdateReq request) {
        productService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 删除产品。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
