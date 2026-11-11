package cn.yq.modules.teaching.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.teaching.biz.DutyManagementBiz;
import cn.yq.modules.teaching.domain.vo.req.ClassDutySaveReq;
import cn.yq.modules.teaching.domain.vo.req.GlobalDutySaveReq;
import cn.yq.modules.teaching.domain.vo.res.DutyManagementDayRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/teaching/duties")
public class DutyManagementController {

    private final DutyManagementBiz dutyManagementBiz;

    /**
     * 查询某一天的值班管理视图。
     * 返回当天需要安排晚自习值班的班级列表，以及 21:00-22:30 的全局值班信息。
     */
    @GetMapping
    public ApiResponse<DutyManagementDayRes> dayView(@RequestParam String dutyDate) {
        return ApiResponse.success(dutyManagementBiz.dayView(dutyDate));
    }

    /**
     * 保存单个班级的晚自习值班安排。
     * 一个老师允许同时值多个班，因此这里只校验老师是否存在，不做冲突拦截。
     */
    @PostMapping("/classes/{classId}")
    public ApiResponse<Void> saveClassDuty(@PathVariable Long classId, @RequestBody @Valid ClassDutySaveReq request) {
        dutyManagementBiz.saveClassDuty(classId, request);
        return ApiResponse.success("保存成功", null);
    }

    /**
     * 保存某一天的全局值班安排。
     * 全局值班固定只有一位老师，对同一天重复保存会覆盖原有记录。
     */
    @PostMapping("/global")
    public ApiResponse<Void> saveGlobalDuty(@RequestBody @Valid GlobalDutySaveReq request) {
        dutyManagementBiz.saveGlobalDuty(request);
        return ApiResponse.success("保存成功", null);
    }
}
