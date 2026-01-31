package org.amalitech.dao;

import org.amalitech.models.Post;
import org.amalitech.interfaces.PostRepository;
import org.amalitech.util.MapRowToPost;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.amalitech.util.CamelToSnake.camelToSnake;

@Repository
public class PostDao implements PostRepository {

    private final DBExecutor db;

    public PostDao(DBExecutor db) {
        this.db = db;
    }

    @Override
    public int save(Post post) {

        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(post, Set.of("id"));

        String clause = insert.getClause();

        String columnsPart = clause.substring(clause.indexOf("(") + 1, clause.indexOf(")"));
        String valuesPart = clause.substring(clause.lastIndexOf("(") + 1, clause.lastIndexOf(")"));

        String sql = "INSERT INTO posts (" + columnsPart + ", search_vector) VALUES (" + valuesPart + ", to_tsvector('english', ? || ' ' || ?)) RETURNING id";

        List<Object> params = new ArrayList<>(insert.getParams());
        params.add(post.getTitle());
        params.add(post.getBody());

        return db.insertAndReturnId(sql, params);
    }

    public Post findById(int id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        List<Object> params = List.of(id);
        var posts = db.query(sql, params, MapRowToPost::mapRowToPost);
        return posts.isEmpty() ? null : posts.get(0);
    }

    public List<Post> findAll() {
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";
        return db.query(sql, new ArrayList<>(), MapRowToPost::mapRowToPost);
    }

    public void update(Post post) {
        post.setUpdatedAt(LocalDateTime.now());

        SqlBuilder.SqlFragment set = SqlBuilder.buildUpdateSetClause(
                post,
                Set.of("id", "createdAt")
        );

        String sql = "UPDATE posts SET " + set.getClause() + ", search_vector = to_tsvector('english', ? || ' ' || ?) WHERE id = ?";

        List<Object> params = new ArrayList<>(set.getParams());
        params.add(post.getTitle());
        params.add(post.getBody());
        params.add(post.getId());

        db.executeUpdate(sql, params);
    }

    public void delete(int id) {
        String table = camelToSnake(Post.class.getSimpleName()) + "s";

        String sql = "DELETE FROM " + table + " WHERE id = ?";

        db.executeUpdate(sql, List.of(id));
    }

    private static final String SEARCH_FROM = " FROM posts p WHERE 1=1";
    private static final String SEARCH_SELECT = "SELECT DISTINCT p.id, p.title, p.body, p.user_id, p.status, p.created_at, p.updated_at";

    @Override
    public List<Post> findByTag(int tagId, int page, int size) {
        String sql = SEARCH_SELECT + SEARCH_FROM +
                " AND p.id IN (SELECT post_id FROM post_tags WHERE tag_id = ?) " +
                " ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
        
        return db.query(sql, List.of(tagId, size, page * size), MapRowToPost::mapRowToPost);
    }

}





