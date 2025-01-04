package com.shared.caching.sharedcachelib;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsService {

   /**
    * Ovo je servis koji prati i bilježi upravljanje metrikama i stastiscima vezane za kesiranje
    * Cilj je osigurati pracejne performansi kesiranja kroz micrometer
    * razdovijo sam u posebnu klasu jer zelim imati bolju modularsnot i fleksibilnost
    * Uz pomoc  Meter registiry -> prikupljamo i zapisujemo metrike
    *
    */

   private MeterRegistry meterRegistry;
   // treads safe
   private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
   private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();


   public void recordMetrics(String metricName, String cacheName, Map<String, String> tags){
      Tags micrometerTags = Tags.of(tags.entrySet().stream()
              .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
              .toArray(String[]::new));
      log.info("Recording metric: name={}, cache={}, tags={}", metricName, cacheName, tags);
      meterRegistry.counter(metricName, micrometerTags).increment();

   }

   public void recordCacheEvent(CacheEvent event){
      switch (event.type()){
         case HIT -> recordCacheHit(event.cacheName());
         case MISS -> recordCacheMiss(event.cacheName());
         case PUT -> recordCachePut(event.cacheName(), event.key());
         case EVICTION -> recordCacheEviction(event.cacheName(), event.key(), event.evictionReason().orElse("Uknown"));
      }

      log.debug("Cache event recorded: type={}, cache={}, key={}, reason={}",
              event.type(), event.cacheName(), event.key(), event.evictionReason().orElse("N/A"));
   }

   private void recordCacheEviction(String cacheName, Object key, String reason) {
      meterRegistry.counter("cache.put", "cache", cacheName, "reason", reason).increment();
      log.warn("Cache eviction: cache={}, key={}, reason={}", cacheName, key, reason);

   }

   private void recordCachePut(String cacheName, Object key) {
      meterRegistry.counter("cache.put", "cache", cacheName).increment();
      log.debug("Cache put: cache={}, key={}", cacheName, key);

   }

   private void recordCacheMiss(String cacheName) {
      cacheMisses.computeIfAbsent(cacheName, key -> new AtomicLong(0)).incrementAndGet();
      updateHitRateMetric(cacheName);
      meterRegistry.counter("cache.miss", "cache", cacheName).increment();
   }

   private void recordCacheHit(String cacheName) {
      cacheHits.computeIfAbsent(cacheName, key -> new AtomicLong(0)).incrementAndGet();
      updateHitRateMetric(cacheName);
      meterRegistry.counter("cache.hit", "cache", cacheName).increment();

   }

   // izracunavam omjer podataka pomocu ukupnih pokusaja "hits i misses"
   private void updateHitRateMetric(String cacheName) {
      long hits = cacheHits.getOrDefault(cacheName, new AtomicLong(0)).longValue();
      long misses = cacheMisses.getOrDefault(cacheName, new AtomicLong(0)).longValue();
      double hitRate = (hits  + misses == 0) ? 0.0 : (double) hits /  (hits + misses);

      // vrijednost guage -> vrijednost koja moze flukutuirati (izmjeniti se)
      meterRegistry.gauge("cache.hit.rate", Tags.of("cache", cacheName), hitRate);
      log.debug("Cache '{}' hit rate updated: {}", cacheName, hitRate);

   }


}
