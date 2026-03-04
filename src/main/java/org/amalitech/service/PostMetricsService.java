package org.amalitech.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.amalitech.algorithm.TrendingSortAlgorithm;
import org.amalitech.entities.Post;
import org.amalitech.repositories.PostRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class PostMetricsService {

    private final PostRepository postRepository;
    private final TrendingSortAlgorithm trendingAlgorithm;

    private final ConcurrentHashMap<Integer, AtomicInteger> viewCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, AtomicInteger> likeCounters = new ConcurrentHashMap<>();

    @Getter
    private volatile List<Post> cachedTrending = List.of();

    public void incrementView(int postId) {
        viewCounters
                .computeIfAbsent(postId, id -> new AtomicInteger())
                .incrementAndGet();
    }

    public void incrementLike(int postId) {
        likeCounters
                .computeIfAbsent(postId, id -> new AtomicInteger())
                .incrementAndGet();
    }


    @Scheduled(fixedRate = 30000)
    public void flushAndRecalculateTrending() {

        viewCounters.forEach((postId, counter) -> {
            int count = counter.getAndSet(0);
            if (count > 0) {
                postRepository.incrementViewCount(postId, count);
            }
        });

        likeCounters.forEach((postId, counter) -> {
            int count = counter.getAndSet(0);
            if (count > 0) {
                postRepository.incrementLikeCount(postId, count);
            }
        });

        List<Post> recentPosts = postRepository.findRecentPublishedPostsForTrending();

        if (!recentPosts.isEmpty()) {
            cachedTrending = trendingAlgorithm.getTopTrending(recentPosts, 10);
        }
    }
}