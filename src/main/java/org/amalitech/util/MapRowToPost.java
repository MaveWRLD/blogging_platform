package org.amalitech.util;

import org.amalitech.models.Post;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MapRowToPost {

    public static Post mapRowToPost(ResultSet rs) throws SQLException {
        Post post = new Post(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getInt("user_id"),
                rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            post.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            post.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return post;
    }
}