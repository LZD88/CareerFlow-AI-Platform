package cn.yq.modules.dataanalysis.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Python Agent 对 SQL 查询结果的解释响应。
 */
@Data
public class PythonDataAnalysisInterpretResultRes {

    /**
     * 整体分析结论。
     */
    private String analysis;

    /**
     * 关键洞察列表。
     */
    private List<String> insights = new ArrayList<>();

    /**
     * 推荐继续追问的问题。
     */
    private List<String> followUpQuestions = new ArrayList<>();

    /**
     * 图表展示建议。
     */
    private ChartSuggestion chartSuggestion;

    /**
     * 前端图表建议。
     */
    @Data
    public static class ChartSuggestion {
        /**
         * 图表类型，例如 bar/line/pie/table。
         */
        private String chartType;
        /**
         * X 轴或分类字段。
         */
        private String xField;
        /**
         * Y 轴或数值字段。
         */
        private String yField;
        /**
         * 系列分组字段。
         */
        private String seriesField;
        /**
         * 推荐该图表的原因。
         */
        private String reason;
    }
}
