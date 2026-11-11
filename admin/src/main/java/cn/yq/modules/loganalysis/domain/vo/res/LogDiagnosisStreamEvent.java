package cn.yq.modules.loganalysis.domain.vo.res;

import lombok.Data;

/**
 * 流式诊断进度事件。
 *
 * <p>后端通过 SSE 把 QUERY_LOGS、STRUCTURE_LOGS、CALL_PYTHON、DONE 等阶段
 * 推给前端，让用户能看到诊断链路正在执行到哪一步。</p>
 */
@Data
public class LogDiagnosisStreamEvent {

    /**
     * 阶段标识，例如 START、QUERY_LOGS、STRUCTURE_LOGS、CALL_PYTHON、DONE、ERROR。
     */
    private String stage;

    /**
     * 阶段状态，例如 STARTED、RUNNING、SUCCESS、DONE、ERROR。
     */
    private String status;

    /**
     * 给前端展示的人类可读提示。
     */
    private String message;

    /**
     * 阶段附加数据。不同阶段结构不同，DONE 阶段通常是完整诊断结果。
     */
    private Object data;

    /**
     * 事件产生时间，毫秒时间戳。
     */
    private Long timestamp;
}
