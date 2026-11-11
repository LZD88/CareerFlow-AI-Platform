package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 学生日常剪影实体。
 */
@Data
public class StudentDailyMomentEntity {

    /** 主键ID */
    private Long id;

    /** 学员编号 */
    private String studentNo;

    /** 素材标题 */
    private String title;

    /** 文字内容 */
    private String contentText;

    /** 文件访问URL（图片/视频等，多个逗号分割） */
    private String fileUrl;

    /** 素材时间 */
    private Date recordTime;

    /** 创建时间 */
    private Date createTime;

    /** 最后更新时间 */
    private Date updateTime;
}
