package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.modules.teaching.domain.entity.CampusEntity;
import cn.yq.modules.teaching.domain.vo.req.CampusCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CampusQueryReq;
import cn.yq.modules.teaching.domain.vo.req.CampusUpdateReq;
import cn.yq.modules.teaching.service.CampusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/campuses")
public class CampusController {

    private final CampusService campusService;

    /**
     * 校区分页查询。
     * 支持按校区名称、地址、负责人以及状态筛选。
     */
    @GetMapping
    public ApiResponse<PageResult<CampusEntity>> page(CampusQueryReq request) {
        return ApiResponse.success(campusService.page(request));
    }

    /**
     * 查询单个校区详情，供编辑弹窗回填使用。
     */
    @GetMapping("/{id}")
    public ApiResponse<CampusEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(campusService.detail(id));
    }

    /**
     * 创建校区。
     */
    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid CampusCreateReq request) {
        return ApiResponse.success("创建成功", campusService.create(request));
    }

    /**
     * 更新校区基础信息。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid CampusUpdateReq request) {
        campusService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 删除校区。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        campusService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
