package org.amalitech.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache for search results.
 * Stores search query results with automatic eviction of eldest entries.
 */
public class SearchCache {

    private static final int DEFAULT_CACHE_SIZE = 50;

    private final LinkedHashMap<String, CachedSearchResult> cache;

    /**
     * Constructor with default cache size.
     */
    public SearchCache() {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Constructor with custom cache size.
     * @param maxSize maximum number of search results to cache
     */
    public SearchCache(int maxSize) {
        this.cache = new LinkedHashMap<String, CachedSearchResult>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedSearchResult> eldest) {
                return size() > maxSize;
            }
        };
    }

    /**
     * Put search results in the cache.
     * @param cacheKey the cache key (search parameters)
     * @param postIds the list of post IDs matching the search
     */
    public void put(String cacheKey, List<Integer> postIds) {
        cache.put(cacheKey, new CachedSearchResult(postIds));
    }

    /**
     * Get search results from the cache.
     * @param cacheKey the cache key
     * @return the cached list of post IDs, or null if not in cache
     */
    public List<Integer> get(String cacheKey) {
        CachedSearchResult result = cache.get(cacheKey);
        return result != null ? result.postIds : null;
    }

    /**
     * Clear all cached results.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get current cache size.
     * @return number of cached entries
     */
    public int size() {
        return cache.size();
    }

    /**
     * Inner class representing a cached search result.
     */
    private static class CachedSearchResult {
        final List<Integer> postIds;
        final LocalDateTime cachedAt;

        CachedSearchResult(List<Integer> postIds) {
            this.postIds = postIds;
            this.cachedAt = LocalDateTime.now();
        }
    }
}

