package com.shared.caching.sharedcachelib;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
@EnableConfigurationProperties(RedisCacheProperties.class)  // Registriramo RedisCacheProperties
public class CentralizedCacheConfiguration implements CachingConfigurer {

    /*
     -> ova klasa sluzi kao orkestrator i pruza centralnu konfiguraciju
       -> cija je jedina zadaca povezati redis i caffeine configuracije
        -> cilj omogucuti hibridnu pristup cache-a roz composoiteCacheManager
     */

    private final RedisCacheConfigurationHelper redisHelper;
    private final CaffeineCacheConfigurationHelper caffeineHelper;
    private final CacheMetricsService cacheMetricsService;

    public CentralizedCacheConfiguration(RedisCacheConfigurationHelper redisHelper, CaffeineCacheConfigurationHelper caffeineHelper, CacheMetricsService cacheMetricsService) {
        this.redisHelper = redisHelper;
        this.caffeineHelper = caffeineHelper;
        this.cacheMetricsService = cacheMetricsService;
    }

    @Override
    @Bean
    public CacheManager cacheManager() {
        // Kreiramo CompositeCacheManager koji podržava i Redis i Caffeine
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(
                createMonitoredCacheManager(caffeineHelper.createCaffeineCacheManager()),
                createMonitoredCacheManager(redisHelper.createRedisCacheManager())
        );
        // Fallback opcija ako cache nije dostupan
        compositeCacheManager.setFallbackToNoOpCache(true);
        return compositeCacheManager;
    }

    private CacheManager createMonitoredCacheManager(CacheManager cacheManager) {
        return new MonitoredCacheManager(cacheManager, cacheMetricsService);

    }

}
