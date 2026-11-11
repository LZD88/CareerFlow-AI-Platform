package cn.yq.modules.teaching.domain.entity;

import lombok.Data;

import java.util.Date;

/**
 * 校区实体。
 * 对应教学管理中的校区基础档案，供列表展示和新增/编辑表单共用。
 */
@Data
public class CampusEntity {

    /** 校区主键 ID */
    private Long id;

    /** 校区名称 */
    private String campusName;

    /** 负责人用户 ID，关联 sys_user.id */
    private Long principalUserId;

    /** 负责人展示名称，由查询时联表补出，不直接落库 */
    private String principalName;

    /** 校区详细地址 */
    private String address;

    /** 校区联系电话 */
    private String contactPhone;

    /** 备注说明 */
    private String remark;

    /** 状态，1 启用，0 禁用 */
    private Integer status;

    /** 创建时间 */
    private Date createdAt;

    /** 更新时间 */
    private Date updatedAt;
}
