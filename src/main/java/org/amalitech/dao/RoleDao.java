package org.amalitech.dao;

import org.amalitech.interfaces.RoleRepository;
import org.amalitech.models.Role;
import org.amalitech.util.RowMappers.MapRowToRole;
import org.amalitech.util.db.DBExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoleDao implements RoleRepository {
    private final DBExecutor db;

    public RoleDao(DBExecutor db) {
        this.db = db;
    }

    @Override
    public Role findByName(String name) {
        String sql = "SELECT * FROM roles WHERE id = ?";
        List<Object> params = List.of(name);
        return db.query(sql, params, MapRowToRole::mapRowToRole)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Role> findByUserId(int userId) {
        String sql = """
            SELECT r.id, r.name
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = ?
        """;
        return db.query(sql, List.of(userId), MapRowToRole::mapRowToRole);
    }
}
