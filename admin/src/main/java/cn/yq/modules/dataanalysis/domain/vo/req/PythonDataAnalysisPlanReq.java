package cn.yq.modules.dataanalysis.domain.vo.req;

import lombok.Data;

/**
 * Java 调 Python Agent 选表的内部请求。
 */
@Data
public class PythonDataAnalysisPlanReq {

    /**
     * 用户自然语言问题。
     */
    private String question;
}
