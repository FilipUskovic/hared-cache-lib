package com.shared.caching.sharedcachelib;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CacheEventListener {

    /** Ova klasa je ulazna točka događaja i ima samo jednu odgovorsnot
     * Prakticki sluzi kao "listener" za događaje vezane uz kesiranje
     * glavni zadatak je prosljediti događdaje do cache metrcisServisa
     */

    private final CacheMetricsService metricsService;

    // kada se dogodi cacheEVENT mis or hit prosljedujemo događaj
    public void onCacheEvent(CacheEvent cacheEvent) {
        // deleigram dogadaj u cachemetrics-u za centralizirano upravljanje
        metricsService.recordCacheEvent(cacheEvent);

        log.debug("Processed cache event: type={}, cache={}, key={}",
                cacheEvent.type(), cacheEvent.cacheName(), cacheEvent.key());
    }


}
