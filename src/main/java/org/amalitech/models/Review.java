package org.amalitech.models;

import java.time.LocalDateTime;

public class Review {

    private int id;
    private int postId;
    private int userId;
    private int rating;
    private String reviewText;
    private LocalDateTime createdAt;

    public Review() {}

    public Review(int id, int postId, int userId, int rating, String reviewText) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

