package com.shared.caching.sharedcachelib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CacheEventListener {
    private static final Logger log = LoggerFactory.getLogger(CacheEventListener.class);

    /** Ova klasa je ulazna točka događaja i ima samo jednu odgovorsnot
     * Prakticki sluzi kao "listener" za događaje vezane uz kesiranje
     * glavni zadatak je prosljediti događdaje do cache metrcisServisa
     */

    private final CacheMetricsService metricsService;

    public CacheEventListener(CacheMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // kada se dogodi cacheEVENT mis or hit prosljedujemo događaj
    public void onCacheEvent(CacheEvent cacheEvent) {
        // deleigram dogadaj u cachemetrics-u za centralizirano upravljanje
        metricsService.recordCacheEvent(cacheEvent);

        log.debug("Processed cache event: type={}, cache={}, key={}",
                cacheEvent.type(), cacheEvent.cacheName(), cacheEvent.key());
    }


}
