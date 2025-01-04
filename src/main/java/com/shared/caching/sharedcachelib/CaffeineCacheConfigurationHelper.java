package com.shared.caching.sharedcachelib;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaffeineCacheConfigurationHelper {

    /**
     *  Ovo je confgiguraciska klasa ua upravljanje i kreiranje "CustomCaffeineCacheManagera" te registracija metrika
     */

    private final CacheEventListener cacheEventListener;
    private final MeterRegistry meterRegistry;

    /* stvara CustomCaffeineCacheManager korsiteci CacheEventListener s različitim strategijama (CacheStrategy)
           -> za korsitenej specificnih cache-a
           -> registira metrike
     */
    public CustomCaffeineCacheManager createCaffeineCacheManager(){
        CustomCaffeineCacheManager cacheManager = new CustomCaffeineCacheManager(cacheEventListener);
        Map<String, Caffeine<Object, Object>> cacheBuilders = configureCaffeineCacheStrategies();
        cacheManager.setCacheBuilders(cacheBuilders);
        registerLocalCacheMetrics(cacheManager);
        return cacheManager;


    }

    // ovdje regisitiramo metrike za svaki cache (pogodci, promaci i velicina cache-a)
    // ako je tip cache-a caffeineCache vraca se antivni cache objekt i registira
    private void registerLocalCacheMetrics(CustomCaffeineCacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if(cache instanceof CaffeineCache caffeineCache){
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                registerCacheMetrics(cacheName, nativeCache);
            }
        });

    }

    private void registerCacheMetrics(String cacheName, com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache) {
        meterRegistry.gauge("cache.size", Tags.of("cache", cacheName), nativeCache, this::getCacheSize);
        meterRegistry.gauge("cache.hit", Tags.of("cache", cacheName), nativeCache, this::getCacheHits);
        meterRegistry.gauge("cache.misses", Tags.of("cache", cacheName), nativeCache, this::getCacheMisses);
        meterRegistry.gauge("cache.evictions", Tags.of("cache", cacheName), nativeCache, this::getCacheEvictions);

    }

    private Map<String, Caffeine<Object, Object>> configureCaffeineCacheStrategies() {
        Map<String, Caffeine<Object, Object>> cacheBuilders = new HashMap<>();
        for (CacheStrategy strategy : CacheStrategy.values()){
            cacheBuilders.put(strategy.getSpec().name(), configureCacheBuilder(strategy.getSpec()));
        }
        return cacheBuilders;
    }

    private Caffeine<Object, Object> configureCacheBuilder(CacheSpec spec) {
        return Caffeine.newBuilder()
                .maximumSize(spec.maxSize())
                .expireAfterWrite(spec.expireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) -> cacheEventListener.onCacheEvent(new CacheEvent(
                        spec.name(),
                        key,
                        CacheEvent.CacheEventType.EVICTION,
                        Optional.of(String.valueOf(cause))
                )));
    }

    private double getCacheSize(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.estimatedSize();
    }

    private double getCacheHits(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.stats().hitCount();
    }

    private double getCacheMisses(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.stats().missCount();
    }

    private double getCacheEvictions(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
        return cache.stats().evictionCount();
    }
}
