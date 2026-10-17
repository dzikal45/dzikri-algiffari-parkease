package com.park.parkease.service.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService<K, V> {
    <T> T lockRequest(String key, Supplier<T> operation);

    void set(K key, V value, Duration ttl);

    Optional<V> get(K key);

    boolean doubleRequest(String requestId, Duration ttl);
}
