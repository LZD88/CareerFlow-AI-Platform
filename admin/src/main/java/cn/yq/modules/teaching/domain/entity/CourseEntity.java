package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 课程实体。
 * 对应教学管理中的课程基础档案。
 */
@Data
public class CourseEntity {

    /** 课程主键 ID */
    private Long id;

    /** 课程名称 */
    private String courseName;

    /** 课程天数 */
    private Integer courseDays;

    /** 飞书资料路径 */
    private String feishuMaterialPath;

    /** 状态，1 启用，0 禁用 */
    private Integer status;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
