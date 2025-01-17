package com.shared.caching.sharedcachelib;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@EnableConfigurationProperties(RedisCacheProperties.class)  // Redis svojstva se sada automatski učitavaju
public class RedisCacheConfigurationHelper {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfigurationHelper.class);

    /* klasa omogucava dinamick-u konfigruacij-u i pracenje metrika za redis cache
     *
     */

    private final RedisConnectionFactory redisConnectionFactory;
    private final MeterRegistry meterRegistry;
    private final RedisCacheProperties redisCacheProperties;

    public RedisCacheConfigurationHelper(RedisConnectionFactory redisConnectionFactory, MeterRegistry meterRegistry, RedisCacheProperties redisCacheProperties) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.meterRegistry = meterRegistry;
        this.redisCacheProperties = redisCacheProperties;
    }


    // centrala tocka upravljanje redis cacheom
    public RedisCacheManager createRedisCacheManager() {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redisCacheProperties.getTimeToLive())
                .prefixCacheNameWith(redisCacheProperties.getKeyPrefix())
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = configureRedisCacheStrategies(defaultConfig);

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware() // svjestan je transakcija i rditi ce u skladu s transakcijama baze podataka
                .build();

        registerRedisMetrics(redisCacheManager);
        return redisCacheManager;
    }

    //   @EventListener(ApplicationReadyEvent.class) // za lazy evul
    private void registerRedisMetrics(RedisCacheManager redisCacheManager) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            Properties info = connection.serverCommands().info(); // Dobivamo Redis informacije
            assert info != null;
            log.info("Redis cache info names : {}", redisCacheManager.getCacheNames());
            meterRegistry.gauge("redis.cache.keys", info, i -> Double.parseDouble(i.getProperty("db0.keys", "0")));
            meterRegistry.gauge("redis.cache.hits", info, i -> Double.parseDouble(i.getProperty("keyspace_hits", "0")));
            meterRegistry.gauge("redis.cache.misses", info, i -> Double.parseDouble(i.getProperty("keyspace_misses", "0")));
            meterRegistry.gauge("redis.cache.evictions", info, i -> Double.parseDouble(i.getProperty("evicted_keys", "0")));
        }
    }

    private Map<String, RedisCacheConfiguration> configureRedisCacheStrategies(RedisCacheConfiguration defaultConfig) {
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        for (CacheStrategy strategy : CacheStrategy.values()) {
            cacheConfigs.put(strategy.getSpec().name(),
                    defaultConfig.entryTtl(strategy.getSpec().expireAfterWrite()));
        }
        return cacheConfigs;
    }

    private RedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}
