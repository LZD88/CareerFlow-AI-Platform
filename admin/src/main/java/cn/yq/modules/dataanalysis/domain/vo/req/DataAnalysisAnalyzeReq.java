package cn.yq.modules.dataanalysis.domain.vo.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 数据分析完整查询请求。
 *
 * <p>前端只需要提交自然语言问题。Java 会调用 Python 生成 SQL，
 * 再由 Java 做 SQL 校验和数据库查询。</p>
 */
@Data
public class DataAnalysisAnalyzeReq {

    /**
     * 用户输入的自然语言分析问题。
     *
     * <p>例如：统计最近一个月每个校区的订单金额。</p>
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 最大返回行数。
     *
     * <p>该值会同时传给 Python 约束生成 SQL，并在 Java SQL Guard 中二次限制。
     * 第一版最大允许 500 行，避免大结果集拖垮页面和 Python 结果解释。</p>
     */
    @Min(value = 1, message = "最大返回行数不能小于1")
    @Max(value = 500, message = "最大返回行数不能大于500")
    private Integer maxRows = 100;
}
