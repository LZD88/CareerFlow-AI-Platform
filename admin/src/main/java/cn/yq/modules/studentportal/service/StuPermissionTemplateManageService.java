package cn.yq.modules.studentportal.service;

import cn.yq.common.api.PageResult;
import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import cn.yq.modules.studentportal.domain.vo.req.StuAssignPermissionReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateCreateReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateQueryReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateUpdateReq;
import cn.yq.modules.studentportal.domain.vo.res.StuPermissionTemplateDetailRes;

public interface StuPermissionTemplateManageService {

    PageResult<StuPermissionTemplateEntity> page(StuPermissionTemplateQueryReq request);

    Long create(StuPermissionTemplateCreateReq request);

    void update(Long id, StuPermissionTemplateUpdateReq request);

    StuPermissionTemplateDetailRes detail(Long id);

    void delete(Long id);

    void assignPermissions(Long id, StuAssignPermissionReq request);
}
