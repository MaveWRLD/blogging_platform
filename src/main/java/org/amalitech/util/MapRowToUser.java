package org.amalitech.util;

import org.amalitech.models.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class MapRowToUser {

    public static User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("role"),
                rs.getString("status")
        );

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
}