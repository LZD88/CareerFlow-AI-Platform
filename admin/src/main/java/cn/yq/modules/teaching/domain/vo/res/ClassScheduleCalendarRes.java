package cn.yq.modules.teaching.domain.vo.res;

import cn.yq.modules.teaching.domain.entity.ClassEntity;
import lombok.Data;

import java.util.List;

@Data
public class ClassScheduleCalendarRes {

    /** 当前班级信息 */
    private ClassEntity classInfo;

    /** 查询年份 */
    private Integer year;

    /** 查询月份 */
    private Integer month;

    /** 当前月份日历数据 */
    private List<ClassScheduleCalendarDayRes> days;
}
