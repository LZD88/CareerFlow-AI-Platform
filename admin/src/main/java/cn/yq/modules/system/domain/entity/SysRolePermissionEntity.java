package cn.yq.modules.system.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysRolePermissionEntity {

    /** 主键ID */
    private Long id;
    /** 角色ID */
    private Long roleId;
    /** 权限ID */
    private Long permissionId;
    /** 关联创建时间 */
    private Date createdAt;
}
