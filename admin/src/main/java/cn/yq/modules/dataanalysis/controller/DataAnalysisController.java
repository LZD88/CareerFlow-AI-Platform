package cn.yq.modules.dataanalysis.controller;

import cn.yq.common.api.ApiResponse;
import cn.yq.modules.dataanalysis.domain.vo.req.DataAnalysisAnalyzeReq;
import cn.yq.modules.dataanalysis.domain.vo.req.DataAnalysisPlanReq;
import cn.yq.modules.dataanalysis.domain.vo.res.DataAnalysisAnalyzeRes;
import cn.yq.modules.dataanalysis.domain.vo.res.DataAnalysisPlanRes;
import cn.yq.modules.dataanalysis.service.DataAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data-analysis")
public class DataAnalysisController {

    private final DataAnalysisService dataAnalysisService;

    @PostMapping("/plan")
    public ApiResponse<DataAnalysisPlanRes> plan(@RequestBody @Valid DataAnalysisPlanReq request) {
        return ApiResponse.success(dataAnalysisService.plan(request));
    }

    /**
     * 数据分析完整入口。
     *
     * <p>Java 负责最终执行数据库查询；Python 只负责生成 SQL 和解释结果。
     * 这样可以把业务库账号、SQL 执行权限和审计都留在 Java 后端。</p>
     */
    @PostMapping("/analyze")
    public ApiResponse<DataAnalysisAnalyzeRes> analyze(@RequestBody @Valid DataAnalysisAnalyzeReq request) {
        return ApiResponse.success(dataAnalysisService.analyze(request));
    }

}
