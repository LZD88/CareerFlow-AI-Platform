package cn.yq.modules.studentportal.domain.vo.res;

import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StuPermissionTreeRes extends StuPermissionEntity {

    private List<StuPermissionTreeRes> children = new ArrayList<>();
}
