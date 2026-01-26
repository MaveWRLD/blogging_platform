package org.amalitech.dao;

import org.amalitech.util.db.DBConnection;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.models.PostTag;
import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.util.DBExecutor;
import org.amalitech.util.SqlBuilder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.amalitech.util.CamelToSnake.camelToSnake;

public class PostTagDao implements PostTagRepository {

    public void save(PostTag postTag) {

        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(postTag, Set.of("id"));

        String sql = "INSERT INTO post_tags " + insert.getClause();

        DBExecutor.execute(sql, insert.getParams());
    }

    public void delete(PostTag postTag) {
        String sql = "DELETE FROM post_tags WHERE post_id = ? AND tag_id = ?";
        DBExecutor.execute(sql, List.of(postTag.getPostId(), postTag.getTagId()));
    }

    public List<Integer> findTagsByPostId(int postId) {
        String sql = "SELECT tag_id FROM post_tags WHERE post_id = ?";
        List<Integer> tagIds = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tagIds.add(rs.getInt("tag_id"));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error fetching tags for post", e);
        }

        return tagIds;
    }

    public List<Integer> findPostsByTagId(int tagId) {
        String sql = "SELECT post_id FROM post_tags WHERE tag_id = ?";
        List<Integer> postIds = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tagId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                postIds.add(rs.getInt("post_id"));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error fetching posts for tag", e);
        }

        return postIds;
    }

    public void deleteAllTagsForPost(int postId) {
        String postColumn = camelToSnake("postId");

        String sql = "DELETE FROM post_tags WHERE " + postColumn + " = ?";

        DBExecutor.execute(sql, List.of(postId));
    }
}
