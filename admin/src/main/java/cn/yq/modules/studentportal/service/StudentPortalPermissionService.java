package cn.yq.modules.studentportal.service;

import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;

import java.util.List;

public interface StudentPortalPermissionService {

    List<StuPermissionEntity> listPermissions(StudentEntity student);

    List<String> listPermissionCodes(StudentEntity student);

    List<StuPermissionEntity> listApiPermissions(StudentEntity student);
}
