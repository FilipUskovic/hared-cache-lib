package com.shared.caching.sharedcachelib;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;


@RequiredArgsConstructor
public class MonitoredCacheManager implements CacheManager {


    /** Wrapper klasa za CacheManager koji dodaje monotoring
     * Delegira orginali cache i urpavlja cache instancama
     */

    private final CacheManager delegate;
    private final CacheMetricsService metricsService;



    @Override
    public Cache getCache(String name) {
       Cache cache = delegate.getCache(name);
       return cache != null ? new MonitoredCache(cache, name, metricsService) : null;
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }

}
