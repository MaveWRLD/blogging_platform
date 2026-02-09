package org.amalitech.dao;

import org.amalitech.models.PostTag;
import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.models.Tag;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.amalitech.util.CamelToSnake.camelToSnake;

@Component
public class PostTagDao implements PostTagRepository {

    private final DBExecutor db;
    private final JdbcTemplate jdbcTemplate;

    public PostTagDao(DBExecutor db, PostDao postDao, JdbcTemplate jdbcTemplate) {
        this.db = db;
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(PostTag postTag) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(postTag, Set.of("id"));
        String sql = "INSERT INTO post_tags " + insert.getClause() + "RETURNING id";
        return db.insertAndReturnId(sql, insert.getParams());
    }

    public void delete(PostTag postTag) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(postTag, Set.of("id"));
        String sql = "DELETE FROM post_tags WHERE " + insert.getClause();
        db.executeUpdate(sql, insert.getParams());
    }

    @Override
    public void addTagsToPost(int postId, List<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?) " +
                "ON CONFLICT (post_id, tag_id) DO NOTHING";

        List<Object[]> batchArgs = tagIds.stream()
                .map(tagId -> new Object[]{postId, tagId})
                .toList();

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public void deleteAllTagsForPost(int postId) {
        String postColumn = camelToSnake("postId");
        String sql = "DELETE FROM post_tags WHERE " + postColumn + " = ?";
        db.executeUpdate(sql, List.of(postId));
    }
}
