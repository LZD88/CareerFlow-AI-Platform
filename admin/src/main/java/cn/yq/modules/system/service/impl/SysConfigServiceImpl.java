package cn.yq.modules.system.service.impl;

import cn.yq.common.api.PageResult;
import cn.yq.common.dataConfig.entity.SysConfigEntity;
import cn.yq.common.dataConfig.mapper.SysConfigMapper;
import cn.yq.common.dataConfig.service.SystemConfigService;
import cn.yq.common.exception.BusinessException;
import cn.yq.modules.system.domain.vo.req.SysConfigCreateReq;
import cn.yq.modules.system.domain.vo.req.SysConfigQueryReq;
import cn.yq.modules.system.domain.vo.req.SysConfigUpdateReq;
import cn.yq.modules.system.service.SysConfigService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final SystemConfigService systemConfigService;

    @Override
    public PageResult<SysConfigEntity> page(SysConfigQueryReq request) {
        Page<SysConfigEntity> page = PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SysConfigEntity> records = sysConfigMapper.selectPage(request.getKeyword());
        return new PageResult<>(page.getTotal(), request.getPageNum(), request.getPageSize(), records);
    }

    @Override
    public SysConfigEntity detail(Long id) {
        return requireConfig(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysConfigCreateReq request) {
        validateUniqueKey(request.getK(), null);
        SysConfigEntity entity = new SysConfigEntity();
        entity.setK(request.getK().trim());
        entity.setV(request.getV().trim());
        sysConfigMapper.insert(entity);
        systemConfigService.evict(entity.getK());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SysConfigUpdateReq request) {
        SysConfigEntity existed = requireConfig(id);
        String oldKey = existed.getK();
        String newKey = request.getK().trim();
        validateUniqueKey(newKey, id);
        existed.setK(newKey);
        existed.setV(request.getV().trim());
        sysConfigMapper.updateById(existed);
        systemConfigService.evict(oldKey);
        systemConfigService.evict(newKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfigEntity existed = requireConfig(id);
        sysConfigMapper.deleteById(id);
        systemConfigService.evict(existed.getK());
    }

    private void validateUniqueKey(String key, Long currentId) {
        SysConfigEntity existed = sysConfigMapper.selectByKey(key.trim());
        if (existed == null) {
            return;
        }
        if (currentId != null && currentId.equals(existed.getId())) {
            return;
        }
        throw new BusinessException("配置Key已存在");
    }

    private SysConfigEntity requireConfig(Long id) {
        SysConfigEntity entity = sysConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("系统配置不存在");
        }
        return entity;
    }
}
