package org.amalitech.dao;

import org.amalitech.util.db.DBConnection;
import org.amalitech.util.exception.DatabaseException;
import org.amalitech.util.exception.NotFoundException;
import org.amalitech.models.Post;
import org.amalitech.models.SortOrder;
import org.amalitech.interfaces.PostRepository;
import org.amalitech.util.DBExecutor;
import org.amalitech.util.SqlBuilder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.amalitech.util.CamelToSnake.camelToSnake;
import static org.amalitech.util.DBExecutor.query;

public class PostDao implements PostRepository {


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

        int generatedId = DBExecutor.execute(sql, params);
        if (generatedId > 0) {
            post.setId(generatedId);
            return generatedId;
        }

        throw new DatabaseException("Failed to retrieve generated post ID");
    }

    public Post findById(int id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        List<Object> params = List.of(id);
        List<Post> results = query(sql, params, this::mapRowToPost);
        if (results.isEmpty()) {
            throw new NotFoundException("Post with ID " + id + " not found");
        }
        return results.get(0);
    }

    public List<Post> findAllPaged(int page, int pageSize) {
        String sql = """
        SELECT * FROM posts
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        """;

        List<Object> params = List.of(pageSize, page * pageSize);

        return DBExecutor.query(sql, params, this::mapRowToPost);
    }

    public List<Post> findAll() {
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";
        return query(sql, new ArrayList<>(), this::mapRowToPost);
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM posts";
        var result = DBExecutor.query(sql, List.of(), rs -> {
            try { return rs.getInt(1); } catch (SQLException e) { throw new RuntimeException(e); }
        });
        return result.isEmpty() ? 0 : result.get(0);
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

        DBExecutor.execute(sql, params);
    }

    public void delete(int id) {
        String table = camelToSnake(Post.class.getSimpleName()) + "s";

        String sql = "DELETE FROM " + table + " WHERE id = ?";

        DBExecutor.execute(sql, List.of(id));
    }

    private static final String SEARCH_FROM = " FROM posts p WHERE 1=1";
    private static final String SEARCH_SELECT = "SELECT DISTINCT p.id, p.title, p.body, p.user_id, p.status, p.created_at, p.updated_at";

    public int countSearch(String query, Set<Integer> tagIds, Set<String> statuses, Integer authorId) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT p.id)").append(SEARCH_FROM);
        appendSearchConditions(sql, query, tagIds, statuses, authorId, params);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParameters(ps, params);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting posts", e);
        }
    }

    @Override
    public List<Post> findByTag(int tagId, int page, int size) {
        String sql = SEARCH_SELECT + SEARCH_FROM +
                " AND p.id IN (SELECT post_id FROM post_tags WHERE tag_id = ?) " +
                " ORDER BY p.created_at DESC LIMIT ? OFFSET ?";
        
        return query(sql, List.of(tagId, size, page * size), this::mapRowToPost);
    }


    public List<Post> search(String query, Set<Integer> tagIds, Set<String> statuses, Integer authorId,
                             SortOrder order, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SEARCH_SELECT).append(SEARCH_FROM);
        appendSearchConditions(sql, query, tagIds, statuses, authorId, params);
        sql.append(" ORDER BY ").append(getSearchOrderByClause(order));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return query(sql.toString(), params, this::mapRowToPost);
    }

    private void appendSearchConditions(StringBuilder sql, String query, Set<Integer> tagIds,
                                        Set<String> statuses, Integer authorId, List<Object> params) {
        if (query != null && !query.isBlank()) {
            sql.append(" AND p.search_vector @@ plainto_tsquery('english', ?)");
            params.add(query.trim());
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            sql.append(" AND p.id IN (SELECT post_id FROM post_tags WHERE tag_id IN (");
            appendInPlaceholders(sql, params, tagIds);
            sql.append("))");
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND p.status IN (");
            appendInPlaceholders(sql, params, statuses);
            sql.append(")");
        }
        if (authorId != null) {
            sql.append(" AND p.user_id = ?");
            params.add(authorId);
        }
    }

    private void appendInPlaceholders(StringBuilder sql, List<Object> params, Iterable<?> values) {
        int i = 0;
        for (Object v : values) {
            if (i++ > 0) sql.append(",");
            sql.append("?");
            params.add(v);
        }
    }

    private String getSearchOrderByClause(SortOrder order) {
        if (order == SortOrder.OLDEST) return "p.created_at ASC";
        if (order == SortOrder.MOST_COMMENTED) return "(SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id) DESC, p.created_at DESC";
        return "p.created_at DESC";
    }

    private void bindParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    public List<Post> listByTag(int tagId, int page, int size) {
        return search(null, Set.of(tagId), Set.of(), null, SortOrder.NEWEST, page, size);
    }

    public int countByTag(int tagId) {
        return countSearch(null, Set.of(tagId), Set.of(), null);
    }

    private Post mapRowToPost(ResultSet rs) throws SQLException {
        Post post = new Post(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getInt("user_id"),
                rs.getString("status"));

        // Map timestamps
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            post.setCreatedAt(createdAt.toLocalDateTime());
        }

        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            post.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return post;
    }
}





