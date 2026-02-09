package org.amalitech.algorithm;

import org.amalitech.models.Post;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

@Component
public class TrendingSortAlgorithm {

    public double calculateTrendingScore(Post post) {
        LocalDateTime now = LocalDateTime.now();

        long hoursOld = Duration.between(post.getCreatedAt(), now).toHours();
        double timeFactor = 1.0 / Math.pow(hoursOld + 2, 1.5);

        int likes = Math.min(post.getLikeCount(), 0);
        int comments = Math.min(post.getCommentCount(), 0);
        int views = Math.min(post.getViewCount(), 0);

        double engagement = (likes * 3.0) + (comments * 5.0) + (views * 0.1);


        return engagement * timeFactor;
    }

    public void sortByTrending(List<Post> posts) {
        for (Post post : posts) {
            double score = calculateTrendingScore(post);
            post.setTrendingScore(score);
        }

        posts.sort((a, b) -> Double.compare(
                b.getTrendingScore() != null ? b.getTrendingScore() : 0.0,
                a.getTrendingScore() != null ? a.getTrendingScore() : 0.0
        ));
    }

    /**
     * Get top K trending posts using heap
     */
    public List<Post> getTopTrending(List<Post> posts, int k) {
        if (posts.size() <= k) {
            sortByTrending(posts);
            return posts;
        }

        PriorityQueue<Post> heap = new PriorityQueue<>(
                (a, b) -> Double.compare(
                        b.getTrendingScore() != null ? b.getTrendingScore() : 0.0,
                        a.getTrendingScore() != null ? a.getTrendingScore() : 0.0
                )
        );

        for (Post post : posts) {
            post.setTrendingScore(calculateTrendingScore(post));
            heap.offer(post);
        }

        List<Post> topK = new ArrayList<>(k);
        for (int i = 0; i < k && !heap.isEmpty(); i++) {
            topK.add(heap.poll());
        }

        return topK;
    }
}