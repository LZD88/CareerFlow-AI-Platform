package cn.yq.modules.studentportal.domain.vo.res;

import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;
import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import lombok.Data;

import java.util.List;

@Data
public class StuPermissionTemplateDetailRes {

    private StuPermissionTemplateEntity template;

    private List<StuPermissionEntity> permissions;
}
