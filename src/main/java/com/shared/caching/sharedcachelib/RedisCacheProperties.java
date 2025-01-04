package com.shared.caching.sharedcachelib;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "spring.cache.redis")
public class RedisCacheProperties {

    private Duration timeToLive = Duration.ofHours(1); // default vrijednost
    private String keyPrefix = "default-prefix";

    public Duration getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Duration timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}



