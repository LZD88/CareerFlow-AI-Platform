package cn.yq.modules.teaching.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.mapper.SysUserMapper;
import cn.yq.modules.teaching.domain.entity.CampusEntity;
import cn.yq.modules.teaching.domain.vo.req.CampusCreateReq;
import cn.yq.modules.teaching.domain.vo.req.CampusQueryReq;
import cn.yq.modules.teaching.domain.vo.req.CampusUpdateReq;
import cn.yq.modules.teaching.mapper.CampusMapper;
import cn.yq.modules.teaching.service.CampusService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 校区管理服务实现。
 * 当前负责人直接复用系统用户表中的用户 ID。
 */
@Service
@RequiredArgsConstructor
public class CampusServiceImpl implements CampusService {

    private final CampusMapper campusMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<CampusEntity> page(CampusQueryReq request) {
        Page<CampusEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        // 列表页需要把负责人名称一并带回，因此查询时直接关联 sys_user。
        List<CampusEntity> records = campusMapper.selectPage(
                request.getKeyword(),
                request.getStatus(),
                request.getPrincipalUserId()
        );
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public CampusEntity detail(Long id) {
        return requireCampus(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CampusCreateReq request) {
        // 保存前先校验负责人是否真实存在，避免页面传入无效用户 ID。
        validatePrincipal(request.getPrincipalUserId());
        CampusEntity campus = new CampusEntity();
        campus.setCampusName(request.getCampusName());
        campus.setPrincipalUserId(request.getPrincipalUserId());
        campus.setAddress(request.getAddress());
        campus.setContactPhone(request.getContactPhone());
        campus.setRemark(request.getRemark());
        campus.setStatus(request.getStatus());
        campus.setCreatedAt(new Date());
        campus.setUpdatedAt(new Date());
        campusMapper.insert(campus);
        return campus.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CampusUpdateReq request) {
        // 编辑时同样校验负责人，防止把历史校区指向一个已不存在的用户。
        validatePrincipal(request.getPrincipalUserId());
        CampusEntity campus = requireCampus(id);
        campus.setCampusName(request.getCampusName());
        campus.setPrincipalUserId(request.getPrincipalUserId());
        campus.setAddress(request.getAddress());
        campus.setContactPhone(request.getContactPhone());
        campus.setRemark(request.getRemark());
        campus.setStatus(request.getStatus());
        campus.setUpdatedAt(new Date());
        campusMapper.updateById(campus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCampus(id);
        campusMapper.deleteById(id);
    }

    private void validatePrincipal(Long principalUserId) {
        if (sysUserMapper.selectById(principalUserId) == null) {
            throw new BusinessException("负责人用户不存在");
        }
    }

    private CampusEntity requireCampus(Long id) {
        // 统一封装存在性校验，避免 controller / service 中反复写空判断。
        CampusEntity campus = campusMapper.selectById(id);
        if (campus == null) {
            throw new BusinessException("校区不存在");
        }
        return campus;
    }
}
