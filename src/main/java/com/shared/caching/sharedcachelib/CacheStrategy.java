package com.shared.caching.sharedcachelib;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum CacheStrategy {
    // definiram 3 strategije kesiranja i svaka korsiti CacheSpec
    REFERENCE(CacheSpec.builder().name("reference").maxSize(1000).expireAfterWrite(Duration.ofHours(1)).build()),
    FREQUENT(CacheSpec.builder().name("frequent").maxSize(10_000).expireAfterWrite(Duration.ofMinutes(10)).build()),
    SEARCH(CacheSpec.builder().name("search").maxSize(1000).expireAfterWrite(Duration.ofMinutes(1)).build());


    private final CacheSpec spec;

    CacheStrategy(CacheSpec spec) {
        this.spec = spec;
    }
}
