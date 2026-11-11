package cn.yq.modules.loganalysis.domain.vo.res;

import lombok.Data;

/**
 * 标准化后的单条日志记录。
 *
 * <p>火山 TLS SDK 返回的日志字段可能包含 {@code __time__}、{@code __content__}
 * 等不同格式，Java 侧统一转换为 time/content，方便前端展示和诊断流程复用。</p>
 */
@Data
public class LogRecordRes {

    /**
     * 日志时间。可能是秒级/毫秒级时间戳，也可能是原始字符串。
     */
    private String time;

    /**
     * 日志正文。
     */
    private String content;
}
