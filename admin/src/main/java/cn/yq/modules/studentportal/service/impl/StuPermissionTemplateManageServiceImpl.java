package cn.yq.modules.studentportal.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.studentportal.domain.entity.StuPermissionTemplateEntity;
import cn.yq.modules.studentportal.domain.entity.StuTemplatePermissionEntity;
import cn.yq.modules.studentportal.domain.vo.req.StuAssignPermissionReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateCreateReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateQueryReq;
import cn.yq.modules.studentportal.domain.vo.req.StuPermissionTemplateUpdateReq;
import cn.yq.modules.studentportal.domain.vo.res.StuPermissionTemplateDetailRes;
import cn.yq.modules.studentportal.mapper.StuPermissionMapper;
import cn.yq.modules.studentportal.mapper.StuPermissionTemplateMapper;
import cn.yq.modules.studentportal.mapper.StuTemplatePermissionMapper;
import cn.yq.modules.studentportal.service.StuPermissionTemplateManageService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StuPermissionTemplateManageServiceImpl implements StuPermissionTemplateManageService {

    private final StuPermissionTemplateMapper stuPermissionTemplateMapper;
    private final StuTemplatePermissionMapper stuTemplatePermissionMapper;
    private final StuPermissionMapper stuPermissionMapper;

    @Override
    public PageResult<StuPermissionTemplateEntity> page(StuPermissionTemplateQueryReq request) {
        Page<StuPermissionTemplateEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<StuPermissionTemplateEntity> records = stuPermissionTemplateMapper.selectPage(
                request.getKeyword(),
                request.getStatus(),
                request.getStudyMode(),
                request.getProductId(),
                request.getClassId()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StuPermissionTemplateCreateReq request) {
        String templateCode = request.getTemplateCode().trim();
        if (stuPermissionTemplateMapper.selectByCode(templateCode) != null) {
            throw new BusinessException("模板编码已存在");
        }
        StuPermissionTemplateEntity entity = new StuPermissionTemplateEntity();
        entity.setTemplateCode(templateCode);
        entity.setTemplateName(request.getTemplateName().trim());
        entity.setStudyMode(normalize(request.getStudyMode()));
        entity.setProductId(request.getProductId());
        entity.setClassId(request.getClassId());
        entity.setDescription(normalize(request.getDescription()));
        entity.setStatus(request.getStatus());
        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());
        stuPermissionTemplateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StuPermissionTemplateUpdateReq request) {
        StuPermissionTemplateEntity entity = requireTemplate(id);
        entity.setTemplateName(request.getTemplateName().trim());
        entity.setStudyMode(normalize(request.getStudyMode()));
        entity.setProductId(request.getProductId());
        entity.setClassId(request.getClassId());
        entity.setDescription(normalize(request.getDescription()));
        entity.setStatus(request.getStatus());
        entity.setUpdatedAt(new Date());
        stuPermissionTemplateMapper.updateById(entity);
    }

    @Override
    public StuPermissionTemplateDetailRes detail(Long id) {
        StuPermissionTemplateEntity entity = requireTemplate(id);
        List<Long> permissionIds = stuTemplatePermissionMapper.selectByTemplateId(id).stream()
                .map(StuTemplatePermissionEntity::getPermissionId)
                .collect(Collectors.toList());

        StuPermissionTemplateDetailRes res = new StuPermissionTemplateDetailRes();
        res.setTemplate(entity);
        res.setPermissions(permissionIds.isEmpty()
                ? Collections.emptyList()
                : stuPermissionMapper.selectAllByIds(permissionIds));
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireTemplate(id);
        stuTemplatePermissionMapper.deleteByTemplateId(id);
        stuPermissionTemplateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, StuAssignPermissionReq request) {
        requireTemplate(id);
        if (stuPermissionMapper.selectAllByIds(request.getPermissionIds()).size() != request.getPermissionIds().size()) {
            throw new BusinessException("存在无效学生权限ID");
        }
        stuTemplatePermissionMapper.deleteByTemplateId(id);
        stuTemplatePermissionMapper.insertTemplatePermissions(id, request.getPermissionIds());
    }

    private StuPermissionTemplateEntity requireTemplate(Long id) {
        StuPermissionTemplateEntity entity = stuPermissionTemplateMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("学生权限模板不存在");
        }
        return entity;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
