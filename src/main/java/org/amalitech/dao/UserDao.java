package org.amalitech.dao;

import org.amalitech.util.exception.NotFoundException;
import org.amalitech.models.User;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.util.DBExecutor;
import static org.amalitech.util.DBExecutor.query;

import org.amalitech.util.SqlBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class UserDao implements UserRepository {

    public int save(User user) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(
                user,
                Set.of("id", "createdAt")
        );
        String sql = "INSERT INTO users " + insert.getClause() + " RETURNING id";
        int generatedId = DBExecutor.execute(sql, insert.getParams());
        user.setId(generatedId);
        return generatedId;
    }

    public User findByUserId(int id) {
        String sql = "SELECT id, username, email, password, role, status, created_at FROM users WHERE id = ?";
        List<User> results = query(sql, List.of(id), this::mapRowToUser);
        if (results.isEmpty()) {
            throw new NotFoundException("User with id " + id + " not found");
        }
        return results.get(0);
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, email, password, role, status, created_at FROM users WHERE username = ?";
        List<User> results = query(sql, List.of(username), this::mapRowToUser);
        if (results.isEmpty()) {
            throw new NotFoundException("Post with username " + username + " not found");
        }
        return results.get(0);
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("status")
        );

        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password = ?, role = ?, status = ? WHERE id = ?";
        DBExecutor.execute(sql, List.of(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getStatus(),
                user.getId()
        ));
    }
}
