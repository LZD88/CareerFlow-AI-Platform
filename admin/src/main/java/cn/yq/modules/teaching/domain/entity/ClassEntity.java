package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 班级实体。
 */
@Data
public class ClassEntity {

    /** 班级主键 ID */
    private Long id;

    /** 班级期数 */
    private Integer classTerm;

    /** 所属校区 ID */
    private Long campusId;

    /** 所属校区名称 */
    private String campusName;

    /** 班主任用户 ID */
    private Long headTeacherId;

    /** 班主任展示名称，联表查询补出 */
    private String headTeacherName;

    /** 班级状态 */
    private String classStatus;

    /** 开班时间 */
    private Date startTime;

    /** 课程 ID */
    private Long courseId;

    /** 课程名称，联表查询补出 */
    private String courseName;

    /** 班级人数 */
    private Integer studentCount;

    /** 是否已生成课表 */
    private Boolean hasSchedule;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
