package cn.yq.modules.loganalysis.service;

import cn.yq.modules.loganalysis.domain.vo.req.LogSearchReq;
import cn.yq.modules.loganalysis.domain.vo.req.LogDiagnoseReq;
import cn.yq.modules.loganalysis.domain.vo.res.LogDiagnosisRes;
import cn.yq.modules.loganalysis.domain.vo.res.LogDiagnosisStreamEvent;
import cn.yq.modules.loganalysis.domain.vo.res.LogSearchRes;

import java.util.function.Consumer;

/**
 * 日志分析服务。
 *
 * <p>当前包含两类能力：</p>
 * <p>1. 直接查询火山日志，验证日志链路和展示原始日志。</p>
 * <p>2. 按 guid 执行诊断链路：查询日志 -> 结构化提取 -> 调 Python Agent -> 返回诊断结果。</p>
 */
public interface LogAnalysisService {

    /**
     * 查询火山 TLS 日志。
     */
    LogSearchRes searchLogs(LogSearchReq request);

    /**
     * 按 guid 诊断，不返回中间进度。
     */
    LogDiagnosisRes diagnoseByGuid(LogDiagnoseReq request);

    /**
     * 按 guid 诊断，并通过 progressConsumer 输出中间进度。
     *
     * <p>这个方法供 SSE 接口使用，前端可以看到每个阶段的状态。</p>
     */
    LogDiagnosisRes diagnoseByGuid(LogDiagnoseReq request, Consumer<LogDiagnosisStreamEvent> progressConsumer);
}
