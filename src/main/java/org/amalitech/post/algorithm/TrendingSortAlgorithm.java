package org.amalitech.post.algorithm;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.amalitech.post.Post;
import org.springframework.stereotype.Component;

@Component
public class TrendingSortAlgorithm {

    public double calculateTrendingScore(Post post) {
        if (post.getCreatedAt() == null) return 0.0;

        long hoursOld = Duration.between(post.getCreatedAt(), Instant.now()).toHours();
        double timeFactor = 1.0 / Math.pow(Math.max(hoursOld, 1) + 2, 1.8);

        double engagement =
                (post.getLikeCount() * 3.0) +
                        (post.getCommentCount() * 5.0) +
                        (post.getViewCount() * 0.1);

        return engagement * timeFactor;
    }


    /**
     * Sort list in-place by trending score (descending)
     */
    public void sortByTrending(List<Post> posts) {
        posts.sort(this::compareByTrendingScore);
    }

    private int compareByTrendingScore(Post a, Post b) {
        double scoreA = calculateTrendingScore(a);
        double scoreB = calculateTrendingScore(b);
        return Double.compare(scoreB, scoreA);
    }

    /**
     * Get top K trending posts efficiently using min-heap
     */
    public List<Post> getTopTrending(List<Post> posts, int k) {

        if (posts == null || posts.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }

        if (k >= posts.size()) {
            return posts.stream()
                    .sorted((a, b) -> Double.compare(
                            calculateTrendingScore(b),
                            calculateTrendingScore(a)
                    ))
                    .toList();
        }

        PriorityQueue<PostScore> minHeap = new PriorityQueue<>(
                Comparator.comparingDouble(ps -> ps.score)
        );

        for (Post post : posts) {
            double score = calculateTrendingScore(post);
            minHeap.offer(new PostScore(post, score));

            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        List<PostScore> topK = new ArrayList<>(minHeap);

        topK.sort((a, b) -> Double.compare(b.score, a.score));

        return topK.stream()
                .map(ps -> ps.post)
                .toList();
    }

    private record PostScore(Post post, double score) {
    }
}