package cn.yq.modules.studentportal.service.impl;

import cn.yq.modules.student.domain.entity.StudentEntity;
import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;
import cn.yq.modules.studentportal.domain.entity.StuTemplatePermissionEntity;
import cn.yq.modules.studentportal.mapper.StuPermissionMapper;
import cn.yq.modules.studentportal.mapper.StuTemplatePermissionMapper;
import cn.yq.modules.studentportal.service.StudentPortalPermissionService;
import cn.yq.modules.studentportal.service.StudentPermissionTemplateBindingManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentPortalPermissionServiceImpl implements StudentPortalPermissionService {

    private static final String PERMISSION_TYPE_API = "API";

    private final StuPermissionMapper stuPermissionMapper;
    private final StuTemplatePermissionMapper stuTemplatePermissionMapper;
    private final StudentPermissionTemplateBindingManageService studentPermissionTemplateBindingManageService;

    @Override
    public List<StuPermissionEntity> listPermissions(StudentEntity student) {
        var template = studentPermissionTemplateBindingManageService.resolveEffectiveTemplate(student);
        if (template == null) {
            return new ArrayList<>();
        }
        List<StuTemplatePermissionEntity> relations = stuTemplatePermissionMapper.selectByTemplateId(template.getId());
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = relations.stream()
                .map(StuTemplatePermissionEntity::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        return stuPermissionMapper.selectAllByIds(permissionIds);
    }

    @Override
    public List<String> listPermissionCodes(StudentEntity student) {
        return listPermissions(student).stream()
                .map(StuPermissionEntity::getPermissionCode)
                .distinct()
                .toList();
    }

    @Override
    public List<StuPermissionEntity> listApiPermissions(StudentEntity student) {
        return listPermissions(student).stream()
                .filter(item -> PERMISSION_TYPE_API.equals(item.getPermissionType()))
                .toList();
    }
}
