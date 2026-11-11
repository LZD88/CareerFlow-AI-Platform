package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 值班安排实体。
 * 班级值班和全局值班共用一张表，classId 为 0 表示全局值班。
 */
@Data
public class DutyAssignmentEntity {

    private Long id;

    private Date dutyDate;

    private String dutyType;

    private Long classId;

    private String startTime;

    private String endTime;

    private Long teacherId;

    private String remark;

    private Date createdAt;

    private Date updatedAt;

    private String teacherName;
}
