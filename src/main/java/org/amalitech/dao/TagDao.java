package org.amalitech.dao;

import org.amalitech.models.Tag;
import org.amalitech.interfaces.TagRepository;
import org.amalitech.util.MapRowToTag;
import org.amalitech.util.db.DBExecutor;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
public class TagDao implements TagRepository {

    private final DBExecutor db;

    public TagDao(DBExecutor db) {
        this.db = db;
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
        return db.query(sql, List.of(id), MapRowToTag::mapRowToTag);
    }

    @Override
    public List<Tag> findAll() {
        String sql = "SELECT * FROM tags ORDER BY name";
        return db.query(sql, new ArrayList<>(), MapRowToTag::mapRowToTag);
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

        db.executeUpdate(sql, params);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM tags WHERE id = ?";
        db.executeUpdate(sql, List.of(id));
    }

    @Override
    public List<Tag> findByName(String name) {
        String sql = "SELECT * FROM tags WHERE name = ?";
        return db.query(sql, List.of(name), MapRowToTag::mapRowToTag);
    }
}

