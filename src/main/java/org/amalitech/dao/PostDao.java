package org.amalitech.dao;

import org.amalitech.models.Post;
import org.amalitech.interfaces.PostRepository;
import org.amalitech.util.RowMappers.PostRowMapper;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class PostDao implements PostRepository {

    private final JdbcOperations jdbcTemplate;

    public PostDao(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int save(Post post) {
        if (post.getExcerpt() == null && post.getBody() != null) {
            post.setExcerpt(post.getBody().length() > 50 ? post.getBody().substring(0, 50) : post.getBody());
        }

        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(post, Set.of("id"));

        String sql = getInsertSql(insert);

        List<Object> params = new ArrayList<>(insert.getParams());
        params.add(post.getTitle());
        params.add(post.getBody());

        Integer generatedId = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                params.toArray()
        );

        return generatedId != null ? generatedId : 0;
    }

    private static String getInsertSql(SqlBuilder.SqlFragment insert) {
        String clause = insert.getClause();

        String columnsPart = clause.substring(clause.indexOf("(") + 1, clause.indexOf(")"));
        String valuesPart = clause.substring(clause.lastIndexOf("(") + 1, clause.lastIndexOf(")"));

        return """
        INSERT INTO posts (%s, search_vector)
        VALUES (%s, to_tsvector('english', ? || ' ' || ?))
        RETURNING id
        """.formatted(columnsPart, valuesPart);
    }

    @Override
    public Optional<Post> findById(int id) {
        String sql = """
                SELECT
                      p.id, p.title, p.body, p.user_id, p.status, p.created_at, p.updated_at,
                      p.excerpt, p.published_at, p.view_count, p.like_count, p.comment_count,
                      COALESCE(array_agg(t.name ORDER BY t.name) FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
                    FROM posts p
                    LEFT JOIN post_tags pt ON pt.post_id = p.id
                    LEFT JOIN tags t       ON t.id = pt.tag_id
                    WHERE p.id = ?
                    GROUP BY p.id;
                """;
        List<Post> posts = jdbcTemplate.query(sql, new PostRowMapper(), id);
        return posts.stream().findFirst();
    }

    @Override
    public List<Post> findAll(int page, int limit) {
        if (page < 0 || limit < 1) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        int offset = page * limit;

        String sql = """
        SELECT
            p.id, p.title, p.body, p.user_id, p.status, p.created_at, p.updated_at,
            p.excerpt, p.published_at, p.view_count, p.like_count, p.comment_count,
            u.username,
            COALESCE(array_agg(t.name ORDER BY t.name) FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
        FROM posts p
        JOIN users u ON p.user_id = u.id
        LEFT JOIN post_tags pt ON pt.post_id = p.id
        LEFT JOIN tags t ON t.id = pt.tag_id
        GROUP BY p.id, u.username, p.created_at
        ORDER BY p.created_at DESC
        LIMIT ? OFFSET ?;
        """;
        return jdbcTemplate.query(sql, new PostRowMapper(), limit, offset);
    }

    public Long countPosts(){
        String countSql = "SELECT COUNT(*) FROM posts";
        return jdbcTemplate.queryForObject(countSql, Long.class);
    }

    @Override
    public List<Post> findRecentForTrending( int limit) {
        String sql = """
            SELECT p.*, u.username as author_name
            FROM posts p
            INNER JOIN users u ON p.user_id = u.id
            WHERE p.status = 'published'
            ORDER BY p.published_at DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new PostRowMapper(), limit);
    }

    /**
     * Increment view count
     */
    public void incrementViewCount(Long postId) {
        String sql = "UPDATE posts SET view_count = view_count + 1 WHERE id = ?";
        jdbcTemplate.update(sql, postId);
    }

    @Override
    public List<Post> findPosts(int page, int size, String tagName, String username, String searchTerm, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        SqlBuilder b = basePostQuery();

        applyCommonFilters(b, username, searchTerm, createdAfter, createdBefore);

        if (isNotBlank(tagName)) {
            b.groupBy("p.id, u.username");
            applyTagFilter(b, tagName);
        } else {
            b.groupBy("p.id, u.username");
        }

        b.orderBy("p.created_at DESC");
        b.limit(size, (long) page * size);

        return jdbcTemplate.query(
                b.getSql(),
                new PostRowMapper(),
                b.getParams().toArray()
        );
    }

    @Override
    public int countPosts(String tagName, String username, String searchTerm, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        SqlBuilder b = new SqlBuilder("""
        SELECT COUNT(DISTINCT p.id) as total
        FROM posts p
        JOIN users u ON p.user_id = u.id
        LEFT JOIN post_tags pt ON pt.post_id = p.id
        LEFT JOIN tags t ON t.id = pt.tag_id
    """);

        applyCommonFilters(b, username, searchTerm, createdAfter, createdBefore);
        applyTagFilter(b, tagName);

        Integer count = jdbcTemplate.queryForObject(
                b.getSql(),
                Integer.class,
                b.getParams().toArray()
        );

        return count != null ? count : 0;
    }

    @Override
    public void update(Post post) {
        if (post.getExcerpt() == null && post.getBody() != null) {
            post.setExcerpt(post.getBody().length() > 500 ? post.getBody().substring(0, 50) : post.getBody());
        }
        if ("published".equals(post.getStatus()) && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        post.setUpdatedAt(LocalDateTime.now());

        SqlBuilder.SqlFragment set = SqlBuilder.buildUpdateSetClause(post, Set.of("id", "createdAt"));

        String sql = "UPDATE posts SET " + set.getClause() + ", search_vector = to_tsvector('english', title || ' ' || body) WHERE id = ?";

        List<Object> params = new ArrayList<>(set.getParams());
        params.add(post.getId());

        jdbcTemplate.update(sql, params.toArray());
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private void applyCommonFilters(SqlBuilder b, String username, String searchTerm, LocalDateTime createdAfter, LocalDateTime createdBefore) {
        if (isNotBlank(username)) {
            b.where("u.username = ?", username.trim());
        }

        if (createdAfter != null) {
            b.and("p.created_at >= ?", createdAfter);
        }

        if (createdBefore != null) {
            b.and("p.created_at < ?", createdBefore);
        }

        if (isNotBlank(searchTerm)) {
            b.and("p.search_vector @@ plainto_tsquery('english', ?)", searchTerm.trim());
        }
    }

    private SqlBuilder basePostQuery() {
        return new SqlBuilder("""
        SELECT
            p.id, p.title, p.body, p.user_id, p.status, p.created_at, p.updated_at,
            p.excerpt, p.published_at, p.view_count, p.like_count, p.comment_count,
            u.username,
            COALESCE(array_agg(t.name ORDER BY t.name) FILTER (WHERE t.name IS NOT NULL), '{}') AS tags
        FROM posts p
        JOIN users u ON p.user_id = u.id
        LEFT JOIN post_tags pt ON pt.post_id = p.id
        LEFT JOIN tags t ON t.id = pt.tag_id
        """);
    }

    private void applyTagFilter(SqlBuilder b, String tagName) {
        if (isNotBlank(tagName)) {
            b.having("COUNT(*) FILTER (WHERE t.name = ?) > 0", tagName.trim());
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}