package org.amalitech.service;

import org.amalitech.interfaces.AppConfig;
import org.amalitech.models.Post;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class PostCacheService {

    private final int maxCacheSize;
    private final int searchResultsCacheSize;

    private final Map<Integer, CachedPost> postCache = new ConcurrentHashMap<>();

    private final TreeMap<LocalDateTime, Set<Integer>> newestIndex = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<LocalDateTime, Set<Integer>> oldestIndex = new TreeMap<>();
    private final Map<Integer, Integer> commentCountIndex = new ConcurrentHashMap<>();

    private final LinkedHashMap<String, CachedSearchResult> searchResultsCache;

    private final LinkedHashMap<Integer, Long> accessOrder;

    /**
     * Constructor that uses AppConfig for cache sizes.
     * @param appConfig the application configuration
     */
    public PostCacheService(AppConfig appConfig) {
        this.maxCacheSize = appConfig.getCacheMaxSize();
        this.searchResultsCacheSize = appConfig.getSearchCacheSize();

        this.searchResultsCache = new LinkedHashMap<String, CachedSearchResult>(
                searchResultsCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedSearchResult> eldest) {
                return size() > searchResultsCacheSize;
            }
        };

        this.accessOrder = new LinkedHashMap<Integer, Long>(maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Long> eldest) {
                if (size() > maxCacheSize) {
                    evictPost(eldest.getKey());
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Default constructor with default configuration values.
     * @deprecated Use PostCacheService(AppConfig) instead
     */
    @Deprecated
    public PostCacheService() {
        this(new org.amalitech.config.PropertiesConfig());
    }


    private static class CachedPost {
        Post post;
        LocalDateTime cachedAt;
        List<Integer> tagIds;

        CachedPost(Post post, List<Integer> tagIds) {
            this.post = post;
            this.tagIds = tagIds != null ? new ArrayList<>(tagIds) : new ArrayList<>();
            this.cachedAt = LocalDateTime.now();
        }
    }


    private static class CachedSearchResult {
        List<Integer> postIds;
        LocalDateTime cachedAt;

        CachedSearchResult(List<Integer> postIds) {
            this.postIds = postIds;
            this.cachedAt = LocalDateTime.now();
        }
    }

    public void invalidate(int postId) {
        CachedPost cached = postCache.remove(postId);
        if (cached != null) {
            LocalDateTime createdAt = cached.post.getCreatedAt();
            if (createdAt != null) {
                removeFromIndex(newestIndex, createdAt, postId);
                removeFromIndex(oldestIndex, createdAt, postId);
            }
            accessOrder.remove(postId);
            commentCountIndex.remove(postId);
        }

        searchResultsCache.clear();
    }

    private void evictPost(int postId) {
        invalidate(postId);
    }

    private void removeFromIndex(TreeMap<LocalDateTime, Set<Integer>> index,
                                 LocalDateTime key, int postId) {
        Set<Integer> ids = index.get(key);
        if (ids != null) {
            ids.remove(postId);
            if (ids.isEmpty()) {
                index.remove(key);
            }
        }
    }
}