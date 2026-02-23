package org.amalitech.entities;

import jakarta.persistence.*;
import lombok.*;
import org.amalitech.enums.PostStatus;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {

    public Post(
            String title, String body, PostStatus status, Instant createdAt, Instant publishedAt
    ) {
        this.title = title;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "title", nullable = false, length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "body", length = Integer.MAX_VALUE)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("now()")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "excerpt", length = 500)
    private String excerpt;

    @Column(name = "published_at")
    private Instant publishedAt;
    @ColumnDefault("0")
    @Column(name = "like_count")
    private int likeCount;

    @ColumnDefault("0")
    @Column(name = "view_count")
    private int viewCount;
    @ColumnDefault("0")
    @Column(name = "comment_count")
    private int commentCount;

    @ManyToMany
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @Column(name = "status", columnDefinition = "post_status")
    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Transient
    private double trendingScore;
}