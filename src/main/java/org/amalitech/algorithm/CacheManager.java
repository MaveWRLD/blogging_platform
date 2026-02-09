package org.amalitech.algorithm;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CacheManager {

    private final LRUCache<String, Object> cache;

    public CacheManager() {
        this.cache = new LRUCache<>(2000);
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public void put(String key, Object value, int ttlSeconds) {
        cache.put(key, value, ttlSeconds);
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }

    public void invalidatePattern(String pattern) {
        cache.invalidatePattern(pattern);
    }

    public void clear() {
        cache.clear();
    }

    public Map<String, Object> getStats() {
        return cache.getStats();
    }
}