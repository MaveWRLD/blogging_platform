package org.amalitech.dao;

import org.amalitech.interfaces.UserRoleRepository;
import org.amalitech.util.db.DBExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRoleDao implements UserRoleRepository{

    private DBExecutor db;

    @Override
    public void assignRoleToUser(int user_id, int role_id) {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        db.insertAndReturnId(sql, List.of(user_id, role_id));
    }

    @Override
    public void removeRoleFromUser(int user_id, int role_id) {
        String sql = "DELETE FROM user_roles WHERE user_id=? AND role_id=?";
        db.executeUpdate(sql, List.of(user_id, role_id));
    }
}
