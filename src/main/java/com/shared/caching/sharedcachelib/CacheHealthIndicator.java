package com.shared.caching.sharedcachelib;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class CacheHealthIndicator extends AbstractHealthIndicator {

    /* klasa koja radi healt check za prikupljeni hit i miss
     * CacheHealthIndicator je springbot Actuator healt koji automatski provjera zdravlje cahce-a
     */

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheHealthIndicator(CacheManager cacheManager, RedisTemplate<String, Object> redisTemplate) {
        super("Cache health check failed");
        this.cacheManager = cacheManager;
        this.redisTemplate = redisTemplate;
    }


    // glavna metoda koja se poziva kada sprpingboot acuator se pokrene, dohvaca sva dostupna cachneName imena i provjerava tipove cache-a
    // te vraca informacije o cache koji se korsiti
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Object> details = new HashMap<>();
        for(String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if(cache instanceof CaffeineCache caffeineCache) {
                details.put(cacheName, getCaffeineStats(caffeineCache));
            } else if (cache instanceof RedisCache redisCache) {
                details.put(cacheName, getRedisStats(redisCache));
            }else {
                details.put(cacheName, "Cache type not supported");
            }
        }
        builder.up().withDetails(details);
    }

    // dohvaca statistiku caffeince uz pomoc naticeCache-a i vraca mapu statistika
    private Map<String, Object> getCaffeineStats(CaffeineCache cache) {
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cache.getNativeCache();
        CacheStats stats = nativeCache.stats();
        return Map.of(
                "size", nativeCache.estimatedSize(),
                "hitRate", stats.hitRate(),
                "missRate", stats.missRate(),
                "evictionCount", stats.evictionCount(),
                "averageLoadPenalty", stats.averageLoadPenalty()
        );
    }

    // mjeri velicinu cachh-a i dohvaca kljuceve pomoc redisTemplate-a korsiti cacheName kao prefkis za dohvacanje svih kljuceva u redicCache-u
    private Map<String, Object> getRedisStats(RedisCache cache) {
        String cacheName = cache.getName();

        // Dobivamo sve ključeve s prefiksom cacheName
        Set<String> keys = redisTemplate.keys(cacheName + "*");
        long size = keys != null ? keys.size() : 0;

        // Možete dodati dodatne statistike
        //TODO Ako je potrebno mogu dodati jos ttl i ostalu metriku
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", size);
        stats.put("keys", keys);

        return stats;
    }


}
