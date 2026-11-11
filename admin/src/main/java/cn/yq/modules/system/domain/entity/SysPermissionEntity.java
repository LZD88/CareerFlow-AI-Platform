package cn.yq.modules.system.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysPermissionEntity {

    /** 权限ID */
    private Long id;
    /** 父权限ID，根节点为0 */
    private Long parentId;
    /** 权限编码，系统内唯一 */
    private String permissionCode;
    /** 权限名称 */
    private String permissionName;
    /** 权限类型，例如API、MENU、BUTTON */
    private String permissionType;
    /** API路径匹配规则，仅API权限使用 */
    private String apiPath;
    /** 排序值，越小越靠前 */
    private Integer sortNum;
    /** 权限描述 */
    private String description;
    /** 状态，1启用，0禁用 */
    private Integer status;
    /** 创建时间 */
    private Date createdAt;
    /** 更新时间 */
    private Date updatedAt;
}
