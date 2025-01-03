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

   private MeterRegistry meterRegistry;
   private final Map<String, AtomicLong> cacheHits = new ConcurrentHashMap<>();
   private final Map<String, AtomicLong> cacheMisses = new ConcurrentHashMap<>();


   public void recordMetrics(String metricName, String cacheName, Map<String, String> tags){
      Tags micrometerTags = Tags.of(tags.entrySet().stream()
              .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
              .toArray(String[]::new));
      log.info("Recording metric: name={}, cache={}, tags={}", metricName, cacheName, tags);
      meterRegistry.counter(metricName, micrometerTags).increment();

   }

}
