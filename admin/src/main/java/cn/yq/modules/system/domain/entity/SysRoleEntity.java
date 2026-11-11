package cn.yq.modules.system.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysRoleEntity {

    /** 角色ID */
    private Long id;
    /** 角色编码，系统内唯一 */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 角色描述 */
    private String description;
    /** 状态，1启用，0禁用 */
    private Integer status;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
}
