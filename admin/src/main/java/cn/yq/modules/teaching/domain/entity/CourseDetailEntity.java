package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 课程详情实体。
 * 每一条记录对应课程中的某一天内容。
 */
@Data
public class CourseDetailEntity {

    /** 主键 ID */
    private Long id;

    /** 所属课程 ID */
    private Long courseId;

    /** 所属阶段 */
    private String stageName;

    /** 第几天课 */
    private Integer dayNum;

    /** 课程内容 */
    private String courseContent;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
