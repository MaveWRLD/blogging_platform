package org.amalitech.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class LRUCache<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(LRUCache.class);

    private final int capacity;
    private final Map<K, CacheEntry<V>> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private long hitCount = 0;
    private long missCount = 0;

    private static class CacheEntry<V> {
        V value;
        LocalDateTime expiryTime;

        CacheEntry(V value, LocalDateTime expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    public LRUCache() {
        this(1000);
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;

        this.cache = new LinkedHashMap<K, CacheEntry<V>>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                boolean shouldRemove = size() > LRUCache.this.capacity;
                if (shouldRemove) {
                    logger.debug("Evicting eldest entry from cache: {}", eldest.getKey());
                }
                return shouldRemove;
            }
        };
    }

    public V get(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cache.get(key);

            if (entry == null) {
                missCount++;
                logger.debug("Cache MISS for key: {}", key);
                return null;
            }

            if (entry.isExpired()) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(key);
                    missCount++;
                    logger.debug("Cache EXPIRED for key: {}", key);
                    return null;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            hitCount++;
            logger.debug("Cache HIT for key: {}", key);
            return entry.value;

        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(K key, V value, int ttlSeconds) {
        lock.writeLock().lock();
        try {
            LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(ttlSeconds);
            cache.put(key, new CacheEntry<>(value, expiryTime));
            logger.debug("Cache PUT for key: {} (TTL: {}s)", key, ttlSeconds);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void invalidate(K key) {
        lock.writeLock().lock();
        try {
            cache.remove(key);
            logger.debug("Cache INVALIDATED for key: {}", key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void invalidatePattern(String pattern) {
        lock.writeLock().lock();
        try {
            cache.keySet().removeIf(key -> key.toString().contains(pattern));
            logger.debug("Cache INVALIDATED for pattern: {}", pattern);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            hitCount = 0;
            missCount = 0;
            logger.info("Cache CLEARED");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Object> getStats() {
        lock.readLock().lock();
        try {
            long total = hitCount + missCount;
            double hitRate = total > 0 ? (double) hitCount / total : 0.0;

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("size", cache.size());
            stats.put("capacity", capacity);
            stats.put("hits", hitCount);
            stats.put("misses", missCount);
            stats.put("hitRate", String.format("%.2f%%", hitRate * 100));

            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }
}