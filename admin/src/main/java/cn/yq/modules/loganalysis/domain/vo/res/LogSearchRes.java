package cn.yq.modules.loganalysis.domain.vo.res;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 火山日志查询结果。
 *
 * <p>这个对象主要服务于“日志查询”页面，也会被按 guid 诊断流程复用，
 * 作为后续结构化提取和 Python Agent 分析的输入。</p>
 */
@Data
public class LogSearchRes {

    /**
     * 实际查询的火山日志 topicId。
     */
    private String topicId;

    /**
     * 实际查询语句。
     */
    private String query;

    /**
     * 查询开始时间，Unix 秒。
     */
    private Long startTime;

    /**
     * 查询结束时间，Unix 秒。
     */
    private Long endTime;

    /**
     * 本次查询限制返回条数。
     */
    private Integer limit;

    /**
     * 火山 TLS 返回的查询类型标识。true 表示分析查询，false 表示普通检索。
     */
    private Boolean isAnalysis;

    /**
     * 本次返回日志条数。
     */
    private Long count;

    /**
     * 命中总数。
     */
    private Long hitCount;

    /**
     * 火山 TLS 返回的查询状态。
     */
    private String resultStatus;

    /**
     * 标准化后的日志列表。
     */
    private List<LogRecordRes> logs = new ArrayList<>();

    /**
     * 保留一份原始响应，便于前期联调和排查 SDK 字段差异。
     */
    private String rawResponse;
}
