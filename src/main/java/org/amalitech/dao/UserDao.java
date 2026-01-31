package org.amalitech.dao;

import org.amalitech.models.User;
import org.amalitech.interfaces.UserRepository;
import org.amalitech.util.MapRowToUser;
import org.amalitech.util.db.DBExecutor;

import org.amalitech.util.db.SqlBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public class UserDao implements UserRepository {

    private final DBExecutor db;

    public UserDao(DBExecutor db) {
        this.db = db;
    }

    @Override
    public int save(User user) {
        SqlBuilder.SqlFragment insert = SqlBuilder.buildInsertClause(
                user,
                Set.of("id", "createdAt")
        );
        String sql = "INSERT INTO users " + insert.getClause() + " RETURNING id";
        return db.insertAndReturnId(sql, insert.getParams());
    }

    public List<User> findByUserId(int id) {
        String sql = "SELECT id, username, email, password, role, status, created_at FROM users WHERE id = ?";
        return db.query(sql, List.of(id), MapRowToUser::mapRowToUser);
    }

    public List<User> findByUsername(String username) {
        String sql = "SELECT id, username, email, password, role, status, created_at FROM users WHERE username = ?";
        return db.query(sql, List.of(username), MapRowToUser::mapRowToUser);
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password = ?, role = ?, status = ? WHERE id = ?";
        db.executeUpdate(sql, List.of(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getStatus(),
                user.getId()
        ));
    }
}
