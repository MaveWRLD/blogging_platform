package org.amalitech.algorithm;

import org.amalitech.entities.Post;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

@Component
public class TrendingSortAlgorithm {

    public double calculateTrendingScore(Post post) {
        if (post.getCreatedAt() == null) {
            return 0.0;
        }

        Instant now = Instant.now();
        long hoursOld = Duration.between(post.getCreatedAt(), now).toHours();

        double timeFactor = 1.0 / Math.pow(Math.max(hoursOld, 1) + 2, 1.8);

        int likes = Math.max(post.getLikeCount(), 0);
        int comments = Math.max(post.getCommentCount(), 0);
        int views = Math.max(post.getViewCount(), 0);

        double engagement = (likes * 3.0) + (comments * 5.0) + (views * 0.1);

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

        PriorityQueue<Post> minHeap = new PriorityQueue<>(
                (a, b) -> Double.compare(calculateTrendingScore(a), calculateTrendingScore(b))
        );

        for (Post post : posts) {
            minHeap.offer(post);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        List<Post> topK = new ArrayList<>(minHeap.size());
        while (!minHeap.isEmpty()) {
            topK.add(minHeap.poll());
        }

        Collections.reverse(topK);
        return topK;
    }
}