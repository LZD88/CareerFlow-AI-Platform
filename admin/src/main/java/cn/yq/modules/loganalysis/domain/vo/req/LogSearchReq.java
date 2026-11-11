package cn.yq.modules.loganalysis.domain.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 火山日志查询请求。
 *
 * <p>前端可以传入查询语句和时间范围直接查询日志；topicId 为空时，
 * 后端会使用系统配置中的默认 topic。</p>
 */
@Data
public class LogSearchReq {

    /**
     * 火山日志 topicId。为空时回退到系统默认配置。
     */
    private String topicId;

    /**
     * 日志检索语句。
     */
    private String query;

    /**
     * 查询开始时间。
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 查询结束时间。
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 返回条数。
     */
    @Min(value = 1, message = "返回条数最少为1")
    @Max(value = 100, message = "返回条数最多为100")
    private Integer limit = 20;
}
