package cn.yq.common.dataConfig.service;

import cn.hutool.core.convert.Convert;
import cn.yq.common.dataConfig.mapper.SysConfigMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SystemConfigService {

    @Resource
    private SysConfigMapper sysConfigMapper;



    private final Cache<String, Object> cache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.SECONDS)
            .maximumSize(10000).build();

    public <T> T get(SystemConfigItem<T> item) {
        Object cached = cache.getIfPresent(item.getKey());

        if (cached != null) {
            return Convert.convert(item.getClazz(), cached);
        }

        String value = sysConfigMapper.getValue(item.getKey());

        if (value == null) {
            cache.put(item.getKey(), item.getDefaultValue());
            // 返回默认值
            return item.getDefaultValue();
        }

        Class<T> clazz = item.getClazz();

        T convert = Convert.convert(clazz, value);
        cache.put(item.getKey(), convert);
        return convert;


    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    public void evict(SystemConfigItem<?> item) {
        cache.invalidate(item.getKey());
    }

    public void evictAll() {
        cache.invalidateAll();
    }
}
