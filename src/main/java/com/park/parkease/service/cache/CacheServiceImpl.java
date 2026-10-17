package com.park.parkease.service.cache;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class CacheServiceImpl<K, V> implements CacheService<K, V> {
    private final ConcurrentHashMap<K, CacheValue<V>> cache =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> requestLocks =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> requestExpirations =
            new ConcurrentHashMap<>();

    private record CacheValue<V>(V value, long expiresAt) {
        boolean isExpired(long now) {
            return expiresAt <= now;
        }
    }

    @Override
    public <T> T lockRequest(String key, Supplier<T> operation) {
        ReentrantLock lock = requestLocks.computeIfAbsent(
                key,
                ignored -> new ReentrantLock()
        );

        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void set(K key, V value, Duration ttl) {
        long now = System.currentTimeMillis();
        cache.put(key, new CacheValue<>(value, expiresAt(now, ttl)));
    }

    @Override
    public Optional<V> get(K key) {
        CacheValue<V> cached = cache.get(key);
        if (cached == null) {
            return Optional.empty();
        }

        if (cached.isExpired(System.currentTimeMillis())) {
            cache.remove(key, cached);
            return Optional.empty();
        }

        return Optional.of(cached.value());
    }

    @Override
    public boolean doubleRequest(String requestId, Duration ttl) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttl.toMillis();
        AtomicBoolean duplicate = new AtomicBoolean(false);

        requestExpirations.compute(requestId, (key, currentExpiresAt) -> {
            if (currentExpiresAt != null && currentExpiresAt > now) {
                duplicate.set(true);
                return currentExpiresAt;
            }
            return expiresAt;
        });

        return duplicate.get();
    }

    private long expiresAt(long now, Duration ttl) {
        long ttlMillis = ttl.toMillis();
        return Math.addExact(now, ttlMillis);
    }
}
