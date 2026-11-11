package cn.yq.modules.dataanalysis.domain.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 数据分析选表请求。
 *
 * <p>这个接口只做“问题 -> 候选表”，不生成 SQL，也不查询数据库。</p>
 */
@Data
public class DataAnalysisPlanReq {

    /**
     * 用户输入的自然语言分析问题。
     */
    @NotBlank(message = "问题不能为空")
    private String question;
}
