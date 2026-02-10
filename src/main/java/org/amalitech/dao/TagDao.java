package org.amalitech.dao;

import org.amalitech.models.Tag;
import org.amalitech.interfaces.TagRepository;
import org.amalitech.util.RowMappers.TagRowMapper;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class TagDao implements TagRepository {

    private final DBExecutor db;
    private final JdbcTemplate jdbcTemplate;


    public TagDao(DBExecutor db, JdbcTemplate jdbcTemplate) {
        this.db = db;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int save(Tag tag) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(tag, Set.of("id"));

        String sql = "INSERT INTO tags " + insert.getClause() + " RETURNING id";
        int generatedId = db.insertAndReturnId(sql, insert.getParams());
        tag.setId(generatedId);
        return generatedId;
    }

    @Override
    public List<Tag> findById(int id) {
        String sql = "SELECT * FROM tags WHERE id = ?";
        return jdbcTemplate.query(sql, new TagRowMapper(), List.of(id));
    }

    @Override
    public List<Tag> findTagsByPostId(int postId) {
        String sql =
            """
                SELECT t.id, t.name
                FROM tags t
                JOIN post_tags pt ON pt.tag_id = t.id
                WHERE pt.post_id = ?
                ORDER BY t.name
            """;

        return jdbcTemplate.query(sql, new TagRowMapper(), postId);
    }

    @Override
    public List<Tag> findAll() {
        String sql = "SELECT * FROM tags ORDER BY name";
        return jdbcTemplate.query(sql, new TagRowMapper());
    }

    @Override
    public void update(Tag tag) {
        SqlBuilder.SqlFragment set = SqlBuilder.buildUpdateSetClause(
                tag,
                Set.of("id")
        );

        String sql = "UPDATE tags SET " + set.getClause() + " WHERE id = ?";

        List<Object> params = new ArrayList<>(set.getParams());
        params.add(tag.getId());

        jdbcTemplate.update(sql, params);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM tags WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Tag> findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name = ?";
        return jdbcTemplate.query(sql, new TagRowMapper(), List.of(name));
    }
}

