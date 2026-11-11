package cn.yq.modules.system.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.studentportal.domain.entity.StuPermissionEntity;
import cn.yq.modules.studentportal.domain.vo.res.StuPermissionTreeRes;
import cn.yq.modules.studentportal.mapper.StuPermissionMapper;
import cn.yq.modules.system.domain.entity.SysPermissionEntity;
import cn.yq.modules.system.domain.vo.req.PermissionCreateReq;
import cn.yq.modules.system.domain.vo.req.PermissionQueryReq;
import cn.yq.modules.system.domain.vo.req.PermissionUpdateReq;
import cn.yq.modules.system.domain.vo.res.PermissionTreeRes;
import cn.yq.modules.system.mapper.SysPermissionMapper;
import cn.yq.modules.system.service.SysPermissionService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理服务实现。
 * 维护系统中的权限点定义，供角色进行绑定。
 */
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;
    private final StuPermissionMapper stuPermissionMapper;

    @Override
    public PageResult<SysPermissionEntity> page(PermissionQueryReq request) {
        Page<SysPermissionEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysPermissionEntity> records = sysPermissionMapper.selectPage(
                request.getKeyword(),
                request.getPermissionType(),
                request.getStatus()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public List<PermissionTreeRes> tree() {
        List<SysPermissionEntity> permissions = sysPermissionMapper.selectAll();
        return buildTree(permissions);
    }

    @Override
    public List<StuPermissionTreeRes> studentTree() {
        return buildStudentTree(stuPermissionMapper.selectAll());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PermissionCreateReq request) {
        if (sysPermissionMapper.selectByCode(request.getPermissionCode()) != null) {
            throw new BusinessException("权限编码已存在");
        }
        validateParent(request.getParentId(), null);
        SysPermissionEntity permission = new SysPermissionEntity();
        permission.setParentId(request.getParentId());
        permission.setPermissionCode(request.getPermissionCode());
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionType(request.getPermissionType());
        permission.setSortNum(request.getSortNum());
        permission.setDescription(request.getDescription());
        permission.setStatus(request.getStatus());
        permission.setCreatedAt(new Date());
        permission.setUpdatedAt(new Date());
        sysPermissionMapper.insert(permission);
        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PermissionUpdateReq request) {
        SysPermissionEntity permission = requirePermission(id);
        validateParent(request.getParentId(), id);
        permission.setParentId(request.getParentId());
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionType(request.getPermissionType());
        permission.setSortNum(request.getSortNum());
        permission.setDescription(request.getDescription());
        permission.setStatus(request.getStatus());
        permission.setUpdatedAt(new Date());
        sysPermissionMapper.updateById(permission);
    }

    @Override
    public SysPermissionEntity detail(Long id) {
        return requirePermission(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requirePermission(id);
        if (sysPermissionMapper.countByParentId(id) > 0) {
            throw new BusinessException("请先删除子权限节点");
        }
        sysPermissionMapper.deleteById(id);
    }

    private SysPermissionEntity requirePermission(Long id) {
        SysPermissionEntity permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }
        return permission;
    }

    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null) {
            throw new BusinessException("父权限不能为空");
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException("父权限不能选择自己");
        }
        if (parentId != 0 && sysPermissionMapper.selectById(parentId) == null) {
            throw new BusinessException("父权限不存在");
        }
    }

    private List<PermissionTreeRes> buildTree(List<SysPermissionEntity> permissions) {
        Map<Long, PermissionTreeRes> nodeMap = new LinkedHashMap<>();
        for (SysPermissionEntity permission : permissions) {
            PermissionTreeRes node = new PermissionTreeRes();
            node.setId(permission.getId());
            node.setParentId(permission.getParentId());
            node.setPermissionCode(permission.getPermissionCode());
            node.setPermissionName(permission.getPermissionName());
            node.setPermissionType(permission.getPermissionType());
            node.setSortNum(permission.getSortNum());
            node.setDescription(permission.getDescription());
            node.setStatus(permission.getStatus());
            node.setCreatedAt(permission.getCreatedAt());
            node.setUpdatedAt(permission.getUpdatedAt());
            nodeMap.put(node.getId(), node);
        }

        List<PermissionTreeRes> roots = new java.util.ArrayList<>();
        for (PermissionTreeRes node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                roots.add(node);
                continue;
            }
            PermissionTreeRes parent = nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            parent.getChildren().add(node);
        }
        return roots;
    }

    private List<StuPermissionTreeRes> buildStudentTree(List<StuPermissionEntity> permissions) {
        Map<Long, StuPermissionTreeRes> nodeMap = new LinkedHashMap<>();
        for (StuPermissionEntity permission : permissions) {
            StuPermissionTreeRes node = new StuPermissionTreeRes();
            node.setId(permission.getId());
            node.setParentId(permission.getParentId());
            node.setPermissionCode(permission.getPermissionCode());
            node.setPermissionName(permission.getPermissionName());
            node.setPermissionType(permission.getPermissionType());
            node.setApiPath(permission.getApiPath());
            node.setRoutePath(permission.getRoutePath());
            node.setIcon(permission.getIcon());
            node.setSortNum(permission.getSortNum());
            node.setDescription(permission.getDescription());
            node.setStatus(permission.getStatus());
            node.setCreatedAt(permission.getCreatedAt());
            node.setUpdatedAt(permission.getUpdatedAt());
            nodeMap.put(node.getId(), node);
        }

        List<StuPermissionTreeRes> roots = new java.util.ArrayList<>();
        for (StuPermissionTreeRes node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                roots.add(node);
                continue;
            }
            StuPermissionTreeRes parent = nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            parent.getChildren().add(node);
        }
        return roots;
    }
}
