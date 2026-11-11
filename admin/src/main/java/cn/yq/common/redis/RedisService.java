package cn.yq.common.redis;

import cn.yq.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisService {
    @Resource
    public RedisTemplate<String, Object> redisTemplate;



    public String getKey(String key) {
        return getKeyCount(key, 0);
    }

    public String getKeyOrNull(String key) {

        Object result = redisTemplate.opsForValue().get(key);
        if (Objects.isNull(result)){
            return null;
        }
        return (String) result;
    }

    private String getKeyCount(String key, Integer count) {
        try {
            return (String) redisTemplate.opsForValue().get(key);
        } catch (QueryTimeoutException e) {
            if (count > 3) {
                throw e;
            }
            return getKeyCount(key, ++count);
        } catch (Exception e) {
            log.error("redis getKey异常 key e:" + key, e);
            throw BusinessException.REDIS_ERROR.newInstance("getKey异常 key:" + key);
        }
    }



    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("redis set异常 key e:" + key, e);
            throw BusinessException.REDIS_ERROR.newInstance("set异常 key:" + key);
        }
    }

    public void set(String key, String value, Long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("redis set异常 key e:" + key, e);
            throw BusinessException.REDIS_ERROR.newInstance("set异常 key:" + key);
        }
    }

    public Boolean setIfAbsent(String key, String value, Long timeout) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("redis setIfAbsent异常 key:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("setIfAbsent异常 key:" + key);
        }
    }

    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("redis hasKey异常 key:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("hasKey异常 key:" + key);
        }
    }

    /**
     * 获取 Object 类型缓存（如 UserInfo），未命中返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getObject(String key) {
        try {
            Object result = redisTemplate.opsForValue().get(key);
            return result == null ? null : (T) result;
        } catch (Exception e) {
            log.error("redis getObject异常 key:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("getObject异常 key:" + key);
        }
    }

    /**
     * 设置 Object 类型缓存并指定过期时间（秒）
     */
    public void setObject(String key, Object value, long timeoutSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("redis setObject异常 key:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("setObject异常 key:" + key);
        }
    }

    public void hset(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            log.error("redis hset异常 key e:{} hashKey:{}", key, hashKey, e);
            throw BusinessException.REDIS_ERROR.newInstance("hset异常 key:" + key);
        }
    }

    public Object hget(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.error("redis hset异常 key e:{} hashKey:{}", key, hashKey, e);
            throw BusinessException.REDIS_ERROR.newInstance("hset异常 key:" + key);
        }
    }

    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("redis hGetAll key  hashKey:" + key + " e:", e);
            throw BusinessException.REDIS_ERROR.newInstance("hGetAll异常 key:" + key);
        }
    }

    public void increment(String key, int i) {
        try {
            redisTemplate.opsForValue().increment(key, i);
        } catch (Exception e) {
            log.error("redis increment异常 key e:" + key, e);
            throw BusinessException.REDIS_ERROR.newInstance("increment异常 key:" + key);
        }
    }

    public void expire(String key, long timeout) {
        try {
            redisTemplate.expire(key, timeout, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis expire异常 key e:" + key, e);
            throw BusinessException.REDIS_ERROR.newInstance("expire异常 key:" + key);
        }
    }


    public Long decrement(String key, int i) {
        try {
            return redisTemplate.opsForValue().decrement(key, i);
        } catch (Exception e) {
            log.error("redis decrement异常 key e:" + key, e);
            throw BusinessException.REDIS_ERROR.newInstance("decrement异常 key:" + key);
        }
    }

    public void sSet(String key, String value) {
        try {
            redisTemplate.opsForSet().add(key, value);
        } catch (Exception e) {
            log.error("redis sSet异常 key e:{} hashKey:{}", key, value, e);
            throw BusinessException.REDIS_ERROR.newInstance("sSet异常 key:" + key);
        }
    }

    public void sSetAll(String key, List<String> value) {
        try {
            for (String v : value) {
                redisTemplate.opsForSet().add(key, v);
            }
        } catch (Exception e) {
            log.error("redis sSet异常 key e:{} hashKey:{}", key, value, e);
            throw BusinessException.REDIS_ERROR.newInstance("sSet异常 key:" + key);
        }
    }

    public Boolean isMember(String key, String hashKey) {
        try {
            return redisTemplate.opsForSet().isMember(key, hashKey);
        } catch (Exception e) {
            log.error("redis sSet异常 key e:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("sSet异常 key:" + key);
        }
    }

    /**
     * 获取Set集合的所有成员
     *
     * @param key Redis Key
     * @return Set集合的所有成员
     */
    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("redis sMembers异常 key e:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("sMembers异常 key:" + key);
        }
    }

    /**
     * 计算两个Set的差集（返回key1中存在但key2中不存在的元素）
     *
     * @param key1 第一个Set的Key
     * @param key2 第二个Set的Key
     * @return 差集结果
     */
    public Set<Object> sDiff(String key1, String key2) {
        try {
            return redisTemplate.opsForSet().difference(key1, key2);
        } catch (Exception e) {
            log.error("redis sDiff异常 key1:{}, key2:{}", key1, key2, e);
            throw BusinessException.REDIS_ERROR.newInstance("sDiff异常 key1:" + key1 + ", key2:" + key2);
        }
    }

    public void geoAdd(String key, String member, Double x, Double y) {

        try {
            Point point = new Point(x, y);
            redisTemplate.opsForGeo().add(key, point, member);
        } catch (Exception e) {
            log.error("redis geoAdd异常 key e:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("geoAdd异常 key:" + key);
        }
    }


    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoSearch(String key, Double x, Double y, Integer limit, Integer dist) {

        try {
            RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                    .newGeoSearchArgs()
                    .includeDistance()
                    .sortAscending()
                    .limit(limit);

            Distance distance = new Distance(dist, Metrics.KILOMETERS);
            return redisTemplate.opsForGeo().search(key, GeoReference.fromCoordinate(x, y), distance, args);
        } catch (Exception e) {
            log.error("redis geoSearch异常 key e:{}", key, e);
            throw BusinessException.REDIS_ERROR.newInstance("geoSearch异常 key:" + key);
        }
    }

}
