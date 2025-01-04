package com.shared.caching.sharedcachelib;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomCaffeineCacheManager extends CaffeineCacheManager {

    /**
     * Ova klasa je priširenje "CaffeineCacheManager" koji dodaje podršku za pračenje cache događaja
     * cacheBuilders -> definiram razlicite strategije
     * eventListener -> reagira na dogadaje
     * svi CacheEvent se prosljeduje kroz onCache metodu
     */

    private final Map<String, Caffeine<Object, Object>> cacheBuilders = new HashMap<>();
    private final CacheEventListener eventListener;

    public CustomCaffeineCacheManager(CacheEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void setCacheBuilders(Map<String, Caffeine<Object, Object>> builders){
        cacheBuilders.putAll(builders);
    }

    /* pracenje događaja iz pomoc  removalListener i metode onCacheEvent
     * ako cache niije unaprijed definiran kreira default velicinu
     * removalListener -> omogucuje racenje izbacivanje kljuceva cache-a i delegira ih listeneru
     */
    @Override
    @NonNull
    protected Cache createCaffeineCache(String name){
        Caffeine<Object, Object> builder = cacheBuilders.getOrDefault(name,
                Caffeine.newBuilder()
                        .maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .removalListener((key, value, cause) -> eventListener.onCacheEvent(
                                new CacheEvent(name, key, CacheEvent.CacheEventType.EVICTION, Optional.of(String.valueOf(cause)))
                        )));
            return new CaffeineCache(name, builder.build());
    }

}
