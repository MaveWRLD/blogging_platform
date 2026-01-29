package org.amalitech.dao;

import org.amalitech.models.Tag;
import org.amalitech.interfaces.TagRepository;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;

import java.sql.*;
import java.util.*;

import static org.amalitech.util.db.DBExecutor.query;

public class TagDao implements TagRepository {

    public int save(Tag tag) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(tag, Set.of("id"));

        String sql = "INSERT INTO tags " + insert.getClause() + " RETURNING id";
        int generatedId = DBExecutor.execute(sql, insert.getParams());
        tag.setId(generatedId);
        return generatedId;
    }

    public Tag findById(int id) {
        String sql = "SELECT * FROM tags WHERE id = ?";
        List<Tag> results = query(sql, List.of(id), this::mapRowToTag);
        return results.get(0);
    }

    public List<Tag> findAll() {
        String sql = "SELECT * FROM tags ORDER BY name";
        return query(sql, new ArrayList<>(), this::mapRowToTag);
    }

    public void update(Tag tag) {
        SqlBuilder.SqlFragment set = SqlBuilder.buildUpdateSetClause(
                tag,
                Set.of("id")
        );

        String sql = "UPDATE tags SET " + set.getClause() + " WHERE id = ?";

        List<Object> params = new ArrayList<>(set.getParams());
        params.add(tag.getId());

        DBExecutor.execute(sql, params);
    }

    public void delete(int id) {
        String sql = "DELETE FROM tags WHERE id = ?";
        DBExecutor.execute(sql, List.of(id));
    }

    public List<Tag> findByPostId(int postId) {
        String sql = "SELECT t.* FROM tags t " +
                     "JOIN post_tags pt ON t.id = pt.tag_id " +
                     "WHERE pt.post_id = ? " +
                     "ORDER BY t.name";
        return query(sql, List.of(postId), this::mapRowToTag);
    }

    public Tag findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name = ?";
        List<Tag> results = query(sql, List.of(name), this::mapRowToTag);
        return results.isEmpty() ? null : results.get(0);
    }

    private Tag mapRowToTag(ResultSet rs) throws SQLException {
        return new Tag(rs.getInt("id"), rs.getString("name"));
    }
}

