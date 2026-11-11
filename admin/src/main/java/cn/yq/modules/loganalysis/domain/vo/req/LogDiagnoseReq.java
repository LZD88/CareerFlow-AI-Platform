package cn.yq.modules.loganalysis.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按 guid 发起日志诊断的请求。
 *
 * <p>guid 由 {@code RequestGuidFilter} 写入请求链路日志，前端输入 guid 后，
 * 后端会回溯最近一段时间内与该 guid 相关的日志。</p>
 */
@Data
public class LogDiagnoseReq {

    /**
     * 请求链路唯一标识。
     */
    @NotBlank(message = "guid 不能为空")
    private String guid;
}
