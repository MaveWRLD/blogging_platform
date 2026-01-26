package org.amalitech.service;

import org.amalitech.config.AppConfig;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


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


    public void put(Post post, List<Integer> tagIds) {
        if (post == null || post.getId() <= 0) return;

        invalidate(post.getId());

        CachedPost cachedPost = new CachedPost(post, tagIds);
        postCache.put(post.getId(), cachedPost);

        LocalDateTime createdAt = post.getCreatedAt() != null ?
                post.getCreatedAt() : LocalDateTime.now();

        newestIndex.computeIfAbsent(createdAt, k -> new HashSet<>()).add(post.getId());
        oldestIndex.computeIfAbsent(createdAt, k -> new HashSet<>()).add(post.getId());

        accessOrder.put(post.getId(), System.currentTimeMillis());
    }

    public Post get(int postId) {
        CachedPost cached = postCache.get(postId);
        if (cached != null) {
            accessOrder.put(postId, System.currentTimeMillis());
            return cached.post;
        }
        return null;
    }

    public List<Post> getSorted(SortOrder order, int limit) {
        List<Post> results = new ArrayList<>();

        switch (order) {
            case NEWEST:
                for (Set<Integer> ids : newestIndex.values()) {
                    for (Integer id : ids) {
                        CachedPost cached = postCache.get(id);
                        if (cached != null) {
                            results.add(cached.post);
                            if (results.size() >= limit) return results;
                        }
                    }
                }
                break;

            case OLDEST:
                for (Set<Integer> ids : oldestIndex.values()) {
                    for (Integer id : ids) {
                        CachedPost cached = postCache.get(id);
                        if (cached != null) {
                            results.add(cached.post);
                            if (results.size() >= limit) return results;
                        }
                    }
                }
                break;

            case MOST_COMMENTED:
                results = postCache.values().stream()
                        .sorted((a, b) -> {
                            int countA = commentCountIndex.getOrDefault(a.post.getId(), 0);
                            int countB = commentCountIndex.getOrDefault(b.post.getId(), 0);
                            return Integer.compare(countB, countA);
                        })
                        .limit(limit)
                        .map(cp -> cp.post)
                        .collect(Collectors.toList());
                break;
        }

        return results;
    }

    public void updateCommentCount(int postId, int count) {
        commentCountIndex.put(postId, count);
    }

    public void putSearchResults(String query, Set<Integer> tagIds,
                                 Set<String> statuses, Integer authorId,
                                 SortOrder order, List<Integer> postIds) {
        String cacheKey = buildSearchCacheKey(query, tagIds, statuses, authorId, order);
        searchResultsCache.put(cacheKey, new CachedSearchResult(postIds));
    }

    public List<Integer> getSearchResults(String query, Set<Integer> tagIds,
                                          Set<String> statuses, Integer authorId,
                                          SortOrder order) {
        String cacheKey = buildSearchCacheKey(query, tagIds, statuses, authorId, order);
        CachedSearchResult cached = searchResultsCache.get(cacheKey);
        return cached != null ? new ArrayList<>(cached.postIds) : null;
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

    public void clear() {
        postCache.clear();
        newestIndex.clear();
        oldestIndex.clear();
        commentCountIndex.clear();
        accessOrder.clear();
        searchResultsCache.clear();
    }

    public CacheStats getStats() {
        return new CacheStats(
                postCache.size(),
                searchResultsCache.size(),
                maxCacheSize,
                searchResultsCacheSize
        );
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

    private String buildSearchCacheKey(String query, Set<Integer> tagIds,
                                       Set<String> statuses, Integer authorId,
                                       SortOrder order) {
        return String.format("%s|%s|%s|%s|%s",
                query != null ? query : "",
                tagIds != null ? tagIds.toString() : "",
                statuses != null ? statuses.toString() : "",
                authorId != null ? authorId : "",
                order != null ? order : "");
    }

    public static class CacheStats {
        public final int postCacheSize;
        public final int searchCacheSize;
        public final int maxPostCacheSize;
        public final int maxSearchCacheSize;

        CacheStats(int postCacheSize, int searchCacheSize,
                   int maxPostCacheSize, int maxSearchCacheSize) {
            this.postCacheSize = postCacheSize;
            this.searchCacheSize = searchCacheSize;
            this.maxPostCacheSize = maxPostCacheSize;
            this.maxSearchCacheSize = maxSearchCacheSize;
        }

        @Override
        public String toString() {
            return String.format("Posts: %d/%d, Searches: %d/%d",
                    postCacheSize, maxPostCacheSize,
                    searchCacheSize, maxSearchCacheSize);
        }
    }
}