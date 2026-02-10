package org.amalitech.dao;

import org.amalitech.models.Role;
import org.amalitech.models.User;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.util.RowMappers.UserRowMapper;
import org.amalitech.util.db.SqlBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserDao implements UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RoleDao roleDao;
    private final UserRoleDao userRoleDao;

    public UserDao(JdbcTemplate jdbcTemplate, RoleDao roleDao, UserRoleDao userRoleDao) {
        this.jdbcTemplate = jdbcTemplate;
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

        Integer userId = jdbcTemplate.queryForObject(sql, Integer.class, insert.getParams().toArray());

        Role role = roleDao.findByName("reader");
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

        return jdbcTemplate.query(sql, new UserRowMapper());
    }

    @Override
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

        List<User> users = jdbcTemplate.query(sql, new UserRowMapper(), id);
        return users.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        String sql =
                """
                    SELECT
                        id, username, email, password, status, created_at,
                        COALESCE(ARRAY_AGG(r.name) FILTER (WHERE r.name IS NOT NULL), '{}') AS roles
                    FROM users u
                    LEFT JOIN user_roles ur ON ur.user_id = u.id
                    LEFT JOIN roles r ON r.id = ur.role_id
                    WHERE u.username = ?
                    GROUP BY u.id
                """;
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper(), username);
        return users.stream().findFirst();
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

        jdbcTemplate.update(sql, params.toArray());
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}