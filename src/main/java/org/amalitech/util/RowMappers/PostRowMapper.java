package org.amalitech.util.RowMappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.amalitech.models.Post;
import org.springframework.jdbc.core.RowMapper;

public class PostRowMapper implements RowMapper<Post> {

    @Override
    public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();

        post.setId(rs.getInt("id"));
        post.setTitle(rs.getString("title"));
        post.setBody(rs.getString("body"));
        post.setUserId(rs.getInt("user_id"));
        post.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            post.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            post.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        post.setExcerpt(rs.getString("excerpt"));
        Timestamp publishedAt = rs.getTimestamp("published_at");
        if (publishedAt != null) {
            post.setPublishedAt(publishedAt.toLocalDateTime());
        }
        post.setViewCount(rs.getInt("view_count"));
        post.setLikeCount(rs.getInt("like_count"));
        post.setCommentCount(rs.getInt("comment_count"));

        return post;
    }
}