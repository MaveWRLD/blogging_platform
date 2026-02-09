package org.amalitech.dao;

import org.amalitech.models.Role;
import org.amalitech.models.User;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.util.RowMappers.MapRowToUser;
import org.amalitech.util.db.DBExecutor;

import org.amalitech.util.db.SqlBuilder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserDao implements UserRepository {

    private final DBExecutor db;
    private final RoleDao roleDao;
    private final UserRoleDao userRoleDao;

    public UserDao(DBExecutor db, RoleDao roleDao, UserRoleDao userRoleDao) {
        this.db = db;
        this.roleDao = roleDao;
        this.userRoleDao = userRoleDao;
    }

    @Override
    public int save(User user) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(
                user,
                Set.of("id", "createdAt")
        );
        String sql = "INSERT INTO users " + insert.getClause() + " RETURNING id";
        Role role = roleDao.findByName("reader");
        int userId = db.insertAndReturnId(sql, insert.getParams());
        if (role != null) {
            userRoleDao.assignRoleToUser(userId, role.getId());
        }
        return userId;
    }

    @Override
    public List<User> findAll() {
        String sql =
                """
                    SELECT
                        u.id, u.username, u.email, u.password, u.status, u.created_at,
                              COALESCE(
                                ARRAY_AGG(DISTINCT r.name) FILTER (WHERE r.name IS NOT NULL),
                                '{}'
                              ) AS roles
                        FROM users u
                        LEFT JOIN user_roles ur ON ur.user_id = u.id
                        LEFT JOIN roles r       ON r.id = ur.role_id
                        GROUP BY
                            u.id, u.username, u.email, u.password, u.status, u.created_at
                        ORDER BY u.id;
                """;
        return db.query(sql, new ArrayList<>(), MapRowToUser::mapRowToUser);
    }

    public Optional<User> findByUserId(int id) {
        String sql =
        """
            SELECT
                u.id, u.username, u.email, u.password, u.status, u.created_at,
                COALESCE(ARRAY_AGG(r.name) FILTER (WHERE r.name IS NOT NULL), '{}') AS roles
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            WHERE u.id = ?
            GROUP BY u.id
        """;
        return db.query(sql, List.of(id), MapRowToUser::mapRowToUser)
                .stream()
                .findFirst();
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, email, password, role, status, created_at FROM users WHERE username = ?";
        return db.query(sql, List.of(username), MapRowToUser::mapRowToUser).stream().findFirst();
    }

    @Override
    public void update(User user) {
        SqlBuilder.SqlFragment set = SqlBuilder.buildUpdateSetClause(
                user,
                Set.of("id", "createdAt", "roles")
        );

        String sql = """
            UPDATE users
            SET %s
            WHERE id = ?
            """.formatted(set.getClause());

        List<Object> params = new ArrayList<>(set.getParams());
        params.add(user.getId());

        db.executeUpdate(sql, params);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        db.executeUpdate(sql, List.of(id));
    }
}
