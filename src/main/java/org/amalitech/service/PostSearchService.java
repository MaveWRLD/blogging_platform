package org.amalitech.service;

import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.interfaces.PostRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for post search functionality.
 * Handles search logic, filtering, and caching.
 * Follows Single Responsibility Principle by separating search concerns.
 */
public class PostSearchService {

    private final PostRepository postRepository;
    private final SearchCache searchCache;

    /**
     * Constructor for PostSearchService.
     * @param postRepository the post repository
     * @param searchCache the search cache
     */
    public PostSearchService(PostRepository postRepository, SearchCache searchCache) {
        this.postRepository = postRepository;
        this.searchCache = searchCache;
    }

    /**
     * Search for posts based on various criteria.
     * @param query search query string
     * @param tagIds set of tag IDs to filter by
     * @param statuses set of post statuses to filter by
     * @param authorId user ID of the post author (optional)
     * @param order sort order for results
     * @param page page number (0-indexed)
     * @param size page size
     * @return list of matching posts
     */
    public List<Post> search(String query, Set<Integer> tagIds, Set<String> statuses,
                            Integer authorId, SortOrder order, int page, int size) {
        String normalizedQuery = normalizeQuery(query);

        String cacheKey = buildSearchCacheKey(normalizedQuery, tagIds, statuses, authorId, order);
        List<Integer> cachedPostIds = searchCache.get(cacheKey);

        List<Post> results;

        if (cachedPostIds != null) {
            results = cachedPostIds.stream()
                    .map(postRepository::findById)
                    .collect(Collectors.toList());
        } else {
            results = postRepository.search(normalizedQuery, tagIds, statuses,
                                          authorId, order, page, size);

            List<Integer> postIds = results.stream()
                    .map(Post::getId)
                    .collect(Collectors.toList());
            searchCache.put(cacheKey, postIds);
        }

        if (cachedPostIds != null) {
            int start = page * size;
            int end = Math.min(start + size, results.size());
            if (start >= results.size()) {
                return List.of();
            }
            return results.subList(start, end);
        }

        return results;
    }

    /**
     * Normalize search query.
     * @param query the raw query
     * @return normalized query
     */
    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        return query.trim().toLowerCase();
    }

    /**
     * Build cache key from search parameters.
     * @param query search query
     * @param tagIds tag IDs
     * @param statuses statuses
     * @param authorId author ID
     * @param order sort order
     * @return cache key
     */
    private String buildSearchCacheKey(String query, Set<Integer> tagIds,
                                      Set<String> statuses, Integer authorId,
                                      SortOrder order) {
        return "search:" + query + ":" +
               (tagIds != null ? tagIds.hashCode() : "null") + ":" +
               (statuses != null ? statuses.hashCode() : "null") + ":" +
               (authorId != null ? authorId : "null") + ":" +
               order;
    }
}

