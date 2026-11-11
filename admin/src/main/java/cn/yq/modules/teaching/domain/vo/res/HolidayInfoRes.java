package cn.yq.modules.teaching.domain.vo.res;

import lombok.Data;

@Data
public class HolidayInfoRes {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 是否节假日/休息日 */
    private Boolean holiday;

    /** 名称，如国庆节/周六 */
    private String name;

    /** 日期类型：0工作日，1周末，2节日，3调休 */
    private Integer type;

    /** 周几，1-7 */
    private Integer week;
}
