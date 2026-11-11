package cn.yq.modules.system.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysUserRoleEntity {

    /** 主键ID */
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 角色ID */
    private Long roleId;
    /** 关联创建时间 */
    private Date createdAt;
}
