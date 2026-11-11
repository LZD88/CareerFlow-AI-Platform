package cn.yq.modules.studentportal.domain.entity;

import lombok.Data;

import java.util.Date;

@Data
public class StuPermissionEntity {

    private Long id;

    private Long parentId;

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private String apiPath;

    private String routePath;

    private String icon;

    private Integer sortNum;

    private String description;

    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
