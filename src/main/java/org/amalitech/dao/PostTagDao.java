package org.amalitech.dao;

import org.amalitech.models.Post;
import org.amalitech.models.PostTag;
import org.amalitech.interfaces.PostTagRepository;
import org.amalitech.models.Tag;
import org.amalitech.util.MapRowToPost;
import org.amalitech.util.MapRowToTag;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.amalitech.util.CamelToSnake.camelToSnake;

@Component
public class PostTagDao implements PostTagRepository {

    private final DBExecutor db;
    private final PostDao postDao;

    public PostTagDao(DBExecutor db, PostDao postDao) {
        this.db = db;
        this.postDao = postDao;
    }

    public int save(PostTag postTag) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(postTag, Set.of("id"));
        String sql = "INSERT INTO post_tags " + insert.getClause() + "RETURNING id";
        return db.insertAndReturnId(sql, insert.getParams());
    }

    public void delete(PostTag postTag) {
        String sql = "DELETE FROM post_tags WHERE post_id = ? AND tag_id = ?";
        db.executeUpdate(sql, List.of(postTag.getPostId(), postTag.getTagId()));
    }

    public List<Tag> findTagsByPostId(int postId) {
        String sql = "SELECT tag_id FROM post_tags WHERE post_id = ?";
        return db.query(sql, new ArrayList<>(), MapRowToTag::mapRowToTag);
    }

    public List<Post> findPostsByTagId(int tagId) {
        String sql = "SELECT post_id FROM post_tags WHERE tag_id = ?";
        return db.query(sql, new ArrayList<>(), MapRowToPost::mapRowToPost);
    }

    public void deleteAllTagsForPost(int postId) {
        String postColumn = camelToSnake("postId");
        String sql = "DELETE FROM post_tags WHERE " + postColumn + " = ?";
        db.executeUpdate(sql, List.of(postId));
    }
}
