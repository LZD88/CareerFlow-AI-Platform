package cn.yq.modules.system.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.common.api.PageResult;
import cn.yq.common.dataConfig.entity.SysConfigEntity;
import cn.yq.modules.system.domain.vo.req.SysConfigCreateReq;
import cn.yq.modules.system.domain.vo.req.SysConfigQueryReq;
import cn.yq.modules.system.domain.vo.req.SysConfigUpdateReq;
import cn.yq.modules.system.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/configs")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping
    public ApiResponse<PageResult<SysConfigEntity>> page(SysConfigQueryReq request) {
        return ApiResponse.success(sysConfigService.page(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SysConfigEntity> detail(@PathVariable Long id) {
        return ApiResponse.success(sysConfigService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid SysConfigCreateReq request) {
        return ApiResponse.success("创建成功", sysConfigService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody @Valid SysConfigUpdateReq request) {
        sysConfigService.update(id, request);
        return ApiResponse.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sysConfigService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
